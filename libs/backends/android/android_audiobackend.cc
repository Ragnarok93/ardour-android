#include "android_audiobackend.h"

#include <algorithm>
#include <cassert>
#include <cstring>

#include "pbd/compose.h"
#include "pbd/error.h"
#include "pbd/i18n.h"
#include "pbd/microseconds.h"
#include "pbd/pthread_utils.h"

#include "ardour/debug.h"
#include "ardour/port_manager.h"

using namespace ARDOUR;

namespace {
std::string s_instance_name;
constexpr const char* kMidiDisabled = "No Android MIDI device";
}

AndroidMidiEvent::AndroidMidiEvent(
    pframes_t timestamp,
    const uint8_t* data,
    size_t size)
    : _timestamp(timestamp)
    , _data(data, data + size)
{
}

AndroidAudioPort::AndroidAudioPort(
    AndroidAudioBackend& backend,
    const std::string& name,
    PortFlags flags)
    : BackendPort(backend, name, flags)
{
    std::memset(_buffer, 0, sizeof(_buffer));
}

void*
AndroidAudioPort::get_buffer(pframes_t nframes)
{
    assert(nframes <= 8192);

    if (is_input()) {
        auto it = get_connections().begin();
        if (it == get_connections().end()) {
            std::memset(_buffer, 0, nframes * sizeof(Sample));
            return _buffer;
        }

        auto source = std::dynamic_pointer_cast<const AndroidAudioPort>(*it);
        assert(source && source->is_output());
        std::memcpy(_buffer, source->const_buffer(), nframes * sizeof(Sample));

        while (++it != get_connections().end()) {
            source = std::dynamic_pointer_cast<const AndroidAudioPort>(*it);
            assert(source && source->is_output());

            Sample* dst = _buffer;
            const Sample* src = source->const_buffer();
            for (pframes_t frame = 0; frame < nframes; ++frame) {
                dst[frame] += src[frame];
            }
        }
    }

    return _buffer;
}

AndroidMidiPort::AndroidMidiPort(
    AndroidAudioBackend& backend,
    const std::string& name,
    PortFlags flags)
    : BackendPort(backend, name, flags)
{
    _buffer.reserve(256);
}

void*
AndroidMidiPort::get_buffer(pframes_t)
{
    if (is_input()) {
        _buffer.clear();

        for (const BackendPortPtr& connected : get_connections()) {
            const auto source = std::dynamic_pointer_cast<const AndroidMidiPort>(connected);
            assert(source && source->is_output());
            _buffer.insert(
                _buffer.end(),
                source->const_buffer()->begin(),
                source->const_buffer()->end()
            );
        }

        std::stable_sort(
            _buffer.begin(),
            _buffer.end(),
            [] (const AndroidMidiEvent& a, const AndroidMidiEvent& b) {
                return a < b;
            }
        );
    }

    return &_buffer;
}

AndroidAudioBackend::AndroidAudioBackend(
    AudioEngine& audio_engine,
    AudioBackendInfo& info)
    : AudioBackend(audio_engine, info)
    , PortEngineSharedImpl(audio_engine, s_instance_name)
    , _instance_name(s_instance_name)
{
    _port_connection_queue.reserve(128);
}

AndroidAudioBackend::~AndroidAudioBackend()
{
    stop();
    clear_ports();
}

std::string
AndroidAudioBackend::name() const
{
    return X_("Android");
}

std::vector<AudioBackend::DeviceStatus>
AndroidAudioBackend::enumerate_devices() const
{
    return { DeviceStatus("Android default", true) };
}

std::vector<float>
AndroidAudioBackend::available_sample_rates(const std::string&) const
{
    return { 44100.f, 48000.f, 88200.f, 96000.f, 192000.f };
}

std::vector<uint32_t>
AndroidAudioBackend::available_buffer_sizes(const std::string&) const
{
    return { 96, 128, 192, 240, 256, 480, 512, 960, 1024, 2048 };
}

int
AndroidAudioBackend::set_device_name(const std::string& device)
{
    if (_running.load(std::memory_order_acquire)) {
        return BackendReinitializationError;
    }

    _device = device.empty() ? "Android default" : device;
    return 0;
}

int
AndroidAudioBackend::set_sample_rate(float sample_rate)
{
    if (_running.load(std::memory_order_acquire) || sample_rate <= 0.f) {
        return -1;
    }

    _sample_rate = sample_rate;
    engine.sample_rate_change(_sample_rate);
    return 0;
}

int
AndroidAudioBackend::set_buffer_size(uint32_t frames)
{
    if (
        _running.load(std::memory_order_acquire) ||
        frames == 0 ||
        frames > kMaxBufferSize
    ) {
        return -1;
    }

    _frames_per_callback = frames;
    engine.buffer_size_change(_frames_per_callback);
    return 0;
}

int
AndroidAudioBackend::set_interleaved(bool yn)
{
    // Ardour's PortEngine buffers remain mono/non-interleaved even though the
    // Oboe device buffer is interleaved at the hardware boundary.
    return yn ? -1 : 0;
}

int
AndroidAudioBackend::set_systemic_input_latency(uint32_t latency)
{
    _systemic_input_latency = latency;
    return 0;
}

int
AndroidAudioBackend::set_systemic_output_latency(uint32_t latency)
{
    _systemic_output_latency = latency;
    return 0;
}

std::vector<std::string>
AndroidAudioBackend::enumerate_midi_options() const
{
    return { kMidiDisabled };
}

int
AndroidAudioBackend::set_midi_option(const std::string& option)
{
    return option == kMidiDisabled ? 0 : -1;
}

bool
AndroidAudioBackend::open_output_stream(oboe::SharingMode sharing_mode)
{
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(sharing_mode);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setSampleRate(static_cast<int32_t>(_sample_rate));
    builder.setChannelCount(static_cast<int32_t>(_output_channels));
    builder.setFramesPerDataCallback(static_cast<int32_t>(_frames_per_callback));
    builder.setDataCallback(this);
    builder.setErrorCallback(this);

    std::shared_ptr<oboe::AudioStream> stream;
    const oboe::Result result = builder.openStream(stream);
    if (result != oboe::Result::OK || !stream) {
        return false;
    }

    _sample_rate = static_cast<float>(stream->getSampleRate());
    _output_channels = static_cast<uint32_t>(stream->getChannelCount());
    _output_stream = std::move(stream);
    return true;
}

int
AndroidAudioBackend::_start(bool)
{
    if (_running.load(std::memory_order_acquire)) {
        return BackendReinitializationError;
    }

    clear_ports();
    _output_stream.reset();

    if (
        !open_output_stream(oboe::SharingMode::Exclusive) &&
        !open_output_stream(oboe::SharingMode::Shared)
    ) {
        return AudioDeviceOpenError;
    }

    if (_output_channels == 0 || _output_channels > 32) {
        _output_stream->close();
        _output_stream.reset();
        return ChannelCountNotSupportedError;
    }

    if (register_system_audio_ports() != 0) {
        _output_stream->close();
        _output_stream.reset();
        return PortRegistrationError;
    }

    engine.sample_rate_change(_sample_rate);
    engine.buffer_size_change(_frames_per_callback);

    if (engine.reestablish_ports()) {
        unregister_ports();
        _output_stream->close();
        _output_stream.reset();
        return PortReconnectError;
    }

    engine.reconnect_ports();
    _port_change_flag.store(0);

    _processed_samples.store(0, std::memory_order_relaxed);
    _cycle_start_us.store(0, std::memory_order_relaxed);
    _dsp_load.store(0.f, std::memory_order_relaxed);
    _dsp_load_calc.set_max_time(_sample_rate, _frames_per_callback);

    manager.registration_callback();
    manager.graph_order_callback();

    _running.store(true, std::memory_order_release);

    if (_output_stream->requestStart() != oboe::Result::OK) {
        _running.store(false, std::memory_order_release);
        unregister_ports();
        _output_stream->close();
        _output_stream.reset();
        return AudioDeviceOpenError;
    }

    return NoError;
}

int
AndroidAudioBackend::stop()
{
    const bool was_running = _running.exchange(false, std::memory_order_acq_rel);

    if (_output_stream) {
        if (was_running) {
            _output_stream->requestStop();
        }
        _output_stream->close();
        _output_stream.reset();
    }

    _active.store(false, std::memory_order_release);
    _main_thread_valid.store(false, std::memory_order_release);

    if (was_running) {
        unregister_ports();
    }

    return 0;
}

int
AndroidAudioBackend::freewheel(bool start_stop)
{
    _freewheel_requested.store(start_stop, std::memory_order_release);
    return 0;
}

size_t
AndroidAudioBackend::raw_buffer_size(DataType type)
{
    switch (type) {
    case DataType::AUDIO:
        return _frames_per_callback * sizeof(Sample);
    case DataType::MIDI:
        return kMaxBufferSize;
    }

    return 0;
}

pframes_t
AndroidAudioBackend::samples_since_cycle_start()
{
    if (!_active.load(std::memory_order_acquire)) {
        return 0;
    }

    const int64_t started = _cycle_start_us.load(std::memory_order_relaxed);
    if (started <= 0) {
        return 0;
    }

    const int64_t elapsed = std::max<int64_t>(
        0,
        PBD::get_microseconds() - started
    );

    const double samples =
        static_cast<double>(elapsed) * static_cast<double>(_sample_rate) / 1000000.0;

    return static_cast<pframes_t>(
        std::min<double>(samples, static_cast<double>(_frames_per_callback))
    );
}

void*
AndroidAudioBackend::process_thread_entry(void* arg)
{
    std::unique_ptr<ThreadData> data(static_cast<ThreadData*>(arg));
    data->func();
    return nullptr;
}

int
AndroidAudioBackend::create_process_thread(std::function<void()> func)
{
    pthread_t thread{};
    ThreadData* data = new ThreadData{std::move(func)};

    if (
        pbd_realtime_pthread_create(
            "Ardour Android Proc",
            PBD_SCHED_FIFO,
            PBD_RT_PRI_PROC,
            PBD_RT_STACKSIZE_PROC,
            &thread,
            process_thread_entry,
            data
        ) != 0
    ) {
        if (
            pbd_pthread_create(
                PBD_RT_STACKSIZE_PROC,
                &thread,
                process_thread_entry,
                data
            ) != 0
        ) {
            delete data;
            return -1;
        }
    }

    _threads.push_back(thread);
    return 0;
}

int
AndroidAudioBackend::join_process_threads()
{
    int result = 0;

    for (pthread_t thread : _threads) {
        void* status = nullptr;
        if (pthread_join(thread, &status) != 0) {
            --result;
        }
    }

    _threads.clear();
    return result;
}

bool
AndroidAudioBackend::in_process_thread()
{
    if (
        _main_thread_valid.load(std::memory_order_acquire) &&
        pthread_equal(_main_thread, pthread_self()) != 0
    ) {
        return true;
    }

    for (pthread_t thread : _threads) {
        if (pthread_equal(thread, pthread_self()) != 0) {
            return true;
        }
    }

    return false;
}

void
AndroidAudioBackend::update_latencies()
{
    port_connect_add_remove_callback();
}

int
AndroidAudioBackend::register_system_audio_ports()
{
    LatencyRange latency;
    latency.min = latency.max = _systemic_output_latency;

    for (uint32_t channel = 0; channel < _output_channels; ++channel) {
        const std::string name = string_compose(
            "system:playback_%1",
            channel + 1
        );

        PortPtr port = add_port(
            name,
            DataType::AUDIO,
            static_cast<PortFlags>(IsInput | IsPhysical | IsTerminal)
        );

        if (!port) {
            return -1;
        }

        set_latency_range(port, true, latency);

        auto android_port = std::dynamic_pointer_cast<AndroidAudioPort>(port);
        android_port->set_hw_port_name(
            string_compose("Android output %1", channel + 1)
        );
        _system_outputs.push_back(android_port);
    }

    // Capture/recording intentionally lands in the next backend milestone.
    // Oboe duplex needs a synchronized input stream feeding the output callback;
    // registering fake capture ports here would make Ardour expose inputs that
    // cannot actually record.
    return 0;
}

BackendPort*
AndroidAudioBackend::port_factory(
    std::string const& name,
    DataType type,
    PortFlags flags)
{
    switch (type) {
    case DataType::AUDIO:
        return new AndroidAudioPort(*this, name, flags);
    case DataType::MIDI:
        return new AndroidMidiPort(*this, name, flags);
    }

    PBD::error
        << string_compose("%1::register_port: invalid data type", _instance_name)
        << endmsg;
    return nullptr;
}

int
AndroidAudioBackend::midi_event_get(
    pframes_t& timestamp,
    size_t& size,
    uint8_t const** data,
    void* port_buffer,
    uint32_t event_index)
{
    if (!data || !port_buffer) {
        return -1;
    }

    auto& source = *static_cast<AndroidMidiBuffer*>(port_buffer);
    if (event_index >= source.size()) {
        return -1;
    }

    const AndroidMidiEvent& event = source[event_index];
    timestamp = event.timestamp();
    size = event.size();
    *data = event.data();
    return 0;
}

int
AndroidAudioBackend::midi_event_put(
    void* port_buffer,
    pframes_t timestamp,
    const uint8_t* data,
    size_t size)
{
    if (!port_buffer || (!data && size != 0)) {
        return -1;
    }

    auto& destination = *static_cast<AndroidMidiBuffer*>(port_buffer);
    destination.emplace_back(timestamp, data, size);
    return 0;
}

uint32_t
AndroidAudioBackend::get_midi_event_count(void* port_buffer)
{
    if (!port_buffer) {
        return 0;
    }

    return static_cast<uint32_t>(
        static_cast<AndroidMidiBuffer*>(port_buffer)->size()
    );
}

void
AndroidAudioBackend::midi_clear(void* port_buffer)
{
    if (port_buffer) {
        static_cast<AndroidMidiBuffer*>(port_buffer)->clear();
    }
}

void
AndroidAudioBackend::set_latency_range(
    PortHandle handle,
    bool for_playback,
    LatencyRange latency)
{
    auto port = std::dynamic_pointer_cast<BackendPort>(handle);
    if (!valid_port(port)) {
        return;
    }

    port->set_latency_range(latency, for_playback);
}

LatencyRange
AndroidAudioBackend::get_latency_range(
    PortHandle handle,
    bool for_playback)
{
    LatencyRange latency;
    latency.min = latency.max = 0;

    auto port = std::dynamic_pointer_cast<BackendPort>(handle);
    if (!valid_port(port)) {
        return latency;
    }

    latency = port->latency_range(for_playback);

    if (
        port->type() == DataType::AUDIO &&
        port->is_physical() &&
        port->is_terminal() &&
        port->is_input() &&
        for_playback
    ) {
        // Until hardware timestamp-based latency measurement is wired, account
        // conservatively for one device callback in addition to user/systemic
        // latency. This is preferable to advertising zero device latency.
        latency.min += _frames_per_callback;
        latency.max += _frames_per_callback;
    }

    return latency;
}

void*
AndroidAudioBackend::get_buffer(PortHandle handle, pframes_t nframes)
{
    auto port = std::dynamic_pointer_cast<BackendPort>(handle);
    assert(port && valid_port(port));
    return port->get_buffer(nframes);
}

void
AndroidAudioBackend::process_port_connection_changes()
{
    bool connections_changed = false;
    bool ports_changed = false;

    if (pthread_mutex_trylock(&_port_callback_mutex) == 0) {
        int expected = 1;
        if (_port_change_flag.compare_exchange_strong(expected, 0)) {
            ports_changed = true;
        }

        connections_changed = !_port_connection_queue.empty();
        process_connection_queue_locked(manager);
        pthread_mutex_unlock(&_port_callback_mutex);
    }

    if (ports_changed) {
        manager.registration_callback();
    }
    if (connections_changed) {
        manager.graph_order_callback();
    }
    if (connections_changed || ports_changed) {
        update_system_port_latencies();
        engine.latency_callback(false);
        engine.latency_callback(true);
    }
}

bool
AndroidAudioBackend::process_cycle(
    float* interleaved_output,
    uint32_t nframes)
{
    if (!interleaved_output || nframes == 0 || nframes > kMaxBufferSize) {
        return false;
    }

    const uint32_t channels = _output_channels;
    std::memset(
        interleaved_output,
        0,
        static_cast<size_t>(nframes) * channels * sizeof(float)
    );

    const bool current_thread =
        _main_thread_valid.load(std::memory_order_acquire) &&
        pthread_equal(_main_thread, pthread_self()) != 0;

    if (!current_thread) {
        _main_thread = pthread_self();
        _main_thread_valid.store(true, std::memory_order_release);
        AudioEngine::thread_init_callback(this);
    }

    const bool requested_freewheel =
        _freewheel_requested.load(std::memory_order_acquire);

    if (requested_freewheel != _freewheel_active) {
        _freewheel_active = requested_freewheel;
        engine.freewheel_callback(_freewheel_active);
    }

    process_port_connection_changes();

    for (const BackendPortPtr& output : _system_outputs) {
        std::memset(
            output->get_buffer(nframes),
            0,
            static_cast<size_t>(nframes) * sizeof(Sample)
        );
    }

    const int64_t cycle_start = PBD::get_microseconds();
    _cycle_start_us.store(cycle_start, std::memory_order_relaxed);
    _dsp_load_calc.set_max_time(_sample_rate, nframes);
    _dsp_load_calc.set_start_timestamp_us(cycle_start);

    if (engine.process_callback(nframes)) {
        return false;
    }

    if (!_freewheel_active) {
        uint32_t channel = 0;
        for (const BackendPortPtr& output : _system_outputs) {
            const Sample* source =
                static_cast<const Sample*>(output->get_buffer(nframes));

            for (uint32_t frame = 0; frame < nframes; ++frame) {
                interleaved_output[
                    static_cast<size_t>(frame) * channels + channel
                ] = source[frame];
            }

            ++channel;
        }
    }

    _processed_samples.fetch_add(nframes, std::memory_order_relaxed);

    _dsp_load_calc.set_stop_timestamp_us(PBD::get_microseconds());
    _dsp_load.store(
        _dsp_load_calc.get_dsp_load(),
        std::memory_order_relaxed
    );

    return true;
}

oboe::DataCallbackResult
AndroidAudioBackend::onAudioReady(
    oboe::AudioStream*,
    void* audio_data,
    int32_t num_frames)
{
    if (
        !_running.load(std::memory_order_acquire) ||
        !audio_data ||
        num_frames <= 0
    ) {
        return oboe::DataCallbackResult::Stop;
    }

    _active.store(true, std::memory_order_release);

    if (!process_cycle(
            static_cast<float*>(audio_data),
            static_cast<uint32_t>(num_frames))) {
        _running.store(false, std::memory_order_release);
        return oboe::DataCallbackResult::Stop;
    }

    return oboe::DataCallbackResult::Continue;
}

void
AndroidAudioBackend::onErrorAfterClose(
    oboe::AudioStream*,
    oboe::Result error)
{
    _active.store(false, std::memory_order_release);
    _running.store(false, std::memory_order_release);

    engine.halted_callback(
        string_compose(
            "Android/Oboe audio stream stopped: %1",
            oboe::convertToText(error)
        ).c_str()
    );
}

XMLNode*
AndroidAudioBackend::get_state() const
{
    XMLNode* node = PortEngineSharedImpl::get_state();
    node->set_property("backend", name());
    node->set_property("device", _device);
    node->set_property("instance", _instance_name);
    return node;
}

int
AndroidAudioBackend::set_state(XMLNode const& node, int version)
{
    if (node.name() != X_("PortEngine")) {
        return -1;
    }

    std::string value;

    if (!node.get_property("backend", value) || value != name()) {
        return -1;
    }
    if (!node.get_property("device", value) || value != _device) {
        return -1;
    }
    if (!node.get_property("instance", value) || value != _instance_name) {
        return -1;
    }

    return PortEngineSharedImpl::set_state(node, version);
}

static std::shared_ptr<AndroidAudioBackend> s_backend_instance;

static std::shared_ptr<AudioBackend>
backend_factory(AudioEngine& engine)
{
    extern AudioBackendInfo android_backend_descriptor;

    if (!s_backend_instance) {
        s_backend_instance.reset(
            new AndroidAudioBackend(engine, android_backend_descriptor)
        );
    }

    return s_backend_instance;
}

static int
instantiate(const std::string& arg1, const std::string&)
{
    s_instance_name = arg1;
    return 0;
}

static int
deinstantiate()
{
    s_backend_instance.reset();
    return 0;
}

static bool
already_configured()
{
    return true;
}

static bool
available()
{
    return true;
}

AudioBackendInfo android_backend_descriptor = {
    "Android (Oboe)",
    instantiate,
    deinstantiate,
    backend_factory,
    already_configured,
    available
};

extern "C" ARDOURBACKEND_API AudioBackendInfo*
descriptor()
{
    return &android_backend_descriptor;
}

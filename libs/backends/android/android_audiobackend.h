#pragma once

#include <atomic>
#include <cstdint>
#include <memory>
#include <set>
#include <string>
#include <vector>

#include <pthread.h>
#include <oboe/Oboe.h>

#include "ardour/audio_backend.h"
#include "ardour/dsp_load_calculator.h"
#include "ardour/port_engine_shared.h"
#include "ardour/types.h"

namespace ARDOUR {

class AndroidAudioBackend;

class AndroidMidiEvent final : public BackendMIDIEvent {
public:
    AndroidMidiEvent(pframes_t timestamp, const uint8_t* data, size_t size);
    AndroidMidiEvent(const AndroidMidiEvent&) = default;

    size_t size() const override { return _data.size(); }
    pframes_t timestamp() const override { return _timestamp; }
    const uint8_t* data() const override { return _data.data(); }

private:
    pframes_t _timestamp;
    std::vector<uint8_t> _data;
};

using AndroidMidiBuffer = std::vector<AndroidMidiEvent>;

class AndroidAudioPort final : public BackendPort {
public:
    AndroidAudioPort(AndroidAudioBackend&, const std::string&, PortFlags);

    DataType type() const override { return DataType::AUDIO; }
    void* get_buffer(pframes_t nframes) override;

    Sample* buffer() { return _buffer; }
    const Sample* const_buffer() const { return _buffer; }

private:
    Sample _buffer[8192];
};

class AndroidMidiPort final : public BackendPort {
public:
    AndroidMidiPort(AndroidAudioBackend&, const std::string&, PortFlags);

    DataType type() const override { return DataType::MIDI; }
    void* get_buffer(pframes_t nframes) override;
    const AndroidMidiBuffer* const_buffer() const { return &_buffer; }

private:
    AndroidMidiBuffer _buffer;
};

class AndroidAudioBackend final
    : public AudioBackend
    , public PortEngineSharedImpl
    , public oboe::AudioStreamDataCallback
    , public oboe::AudioStreamErrorCallback
{
public:
    AndroidAudioBackend(AudioEngine&, AudioBackendInfo&);
    ~AndroidAudioBackend() override;

    std::string name() const override;
    bool is_realtime() const override { return true; }

    bool requires_driver_selection() const override { return false; }
    std::string driver_name() const override { return "AAudio / Oboe"; }

    std::vector<DeviceStatus> enumerate_devices() const override;
    std::vector<float> available_sample_rates(const std::string&) const override;
    std::vector<uint32_t> available_buffer_sizes(const std::string&) const override;

    bool can_change_sample_rate_when_running() const override { return false; }
    bool can_change_buffer_size_when_running() const override { return false; }
    bool can_measure_systemic_latency() const override { return false; }

    int set_device_name(const std::string&) override;
    int set_sample_rate(float) override;
    int set_buffer_size(uint32_t) override;
    int set_interleaved(bool yn) override;
    int set_systemic_input_latency(uint32_t) override;
    int set_systemic_output_latency(uint32_t) override;
    int set_systemic_midi_input_latency(std::string const, uint32_t) override { return 0; }
    int set_systemic_midi_output_latency(std::string const, uint32_t) override { return 0; }

    int reset_device() override { return 0; }

    std::string device_name() const override { return _device; }
    float sample_rate() const override { return _sample_rate; }
    uint32_t buffer_size() const override { return _frames_per_callback; }
    bool interleaved() const override { return false; }
    uint32_t systemic_input_latency() const override { return _systemic_input_latency; }
    uint32_t systemic_output_latency() const override { return _systemic_output_latency; }
    uint32_t systemic_midi_input_latency(std::string const) const override { return 0; }
    uint32_t systemic_midi_output_latency(std::string const) const override { return 0; }

    std::string control_app_name() const override { return {}; }
    void launch_control_app() override {}

    std::vector<std::string> enumerate_midi_options() const override;
    int set_midi_option(const std::string&) override;
    std::string midi_option() const override { return "No Android MIDI device"; }
    std::vector<DeviceStatus> enumerate_midi_devices() const override { return {}; }
    int set_midi_device_enabled(std::string const, bool) override { return 0; }
    bool midi_device_enabled(std::string const) const override { return false; }
    bool can_set_systemic_midi_latencies() const override { return false; }

protected:
    int _start(bool for_latency_measurement) override;

public:
    int stop() override;
    int freewheel(bool start_stop) override;
    float dsp_load() const override { return 100.f * _dsp_load.load(std::memory_order_relaxed); }
    size_t raw_buffer_size(DataType) override;

    samplepos_t sample_time() override { return _processed_samples.load(std::memory_order_relaxed); }
    samplepos_t sample_time_at_cycle_start() override { return sample_time(); }
    pframes_t samples_since_cycle_start() override;

    int create_process_thread(std::function<void()> func) override;
    int join_process_threads() override;
    bool in_process_thread() override;
    uint32_t process_thread_count() override { return static_cast<uint32_t>(_threads.size()); }
    void update_latencies() override;

    void* private_handle() const override { return _output_stream.get(); }
    const std::string& my_name() const override { return _instance_name; }

    bool port_is_physical(PortHandle ph) const override { return PortEngineSharedImpl::port_is_physical(ph); }
    void get_physical_outputs(DataType t, std::vector<std::string>& r) override { PortEngineSharedImpl::get_physical_outputs(t, r); }
    void get_physical_inputs(DataType t, std::vector<std::string>& r) override { PortEngineSharedImpl::get_physical_inputs(t, r); }
    ChanCount n_physical_outputs() const override { return PortEngineSharedImpl::n_physical_outputs(); }
    ChanCount n_physical_inputs() const override { return PortEngineSharedImpl::n_physical_inputs(); }
    uint32_t port_name_size() const override { return PortEngineSharedImpl::port_name_size(); }
    int set_port_name(PortHandle ph, const std::string& n) override { return PortEngineSharedImpl::set_port_name(ph, n); }
    std::string get_port_name(PortHandle ph) const override { return PortEngineSharedImpl::get_port_name(ph); }
    PortFlags get_port_flags(PortHandle ph) const override { return PortEngineSharedImpl::get_port_flags(ph); }
    PortPtr get_port_by_name(const std::string& n) const override { return PortEngineSharedImpl::get_port_by_name(n); }
    int get_port_property(PortHandle ph, const std::string& k, std::string& v, std::string& t) const override { return PortEngineSharedImpl::get_port_property(ph, k, v, t); }
    int set_port_property(PortHandle ph, const std::string& k, const std::string& v, const std::string& t) override { return PortEngineSharedImpl::set_port_property(ph, k, v, t); }
    int get_ports(const std::string& p, DataType t, PortFlags f, std::vector<std::string>& r) const override { return PortEngineSharedImpl::get_ports(p, t, f, r); }
    DataType port_data_type(PortHandle ph) const override { return PortEngineSharedImpl::port_data_type(ph); }
    PortPtr register_port(const std::string& n, DataType t, PortFlags f) override { return PortEngineSharedImpl::register_port(n, t, f); }
    void unregister_port(PortHandle ph) override { if (_running.load()) PortEngineSharedImpl::unregister_port(ph); }
    int connect(const std::string& a, const std::string& b) override { return PortEngineSharedImpl::connect(a, b); }
    int disconnect(const std::string& a, const std::string& b) override { return PortEngineSharedImpl::disconnect(a, b); }
    int connect(PortHandle ph, const std::string& other) override { return PortEngineSharedImpl::connect(ph, other); }
    int disconnect(PortHandle ph, const std::string& other) override { return PortEngineSharedImpl::disconnect(ph, other); }
    int disconnect_all(PortHandle ph) override { return PortEngineSharedImpl::disconnect_all(ph); }
    bool connected(PortHandle ph, bool safe) override { return PortEngineSharedImpl::connected(ph, safe); }
    bool connected_to(PortHandle ph, const std::string& o, bool safe) override { return PortEngineSharedImpl::connected_to(ph, o, safe); }
    bool physically_connected(PortHandle ph, bool safe) override { return PortEngineSharedImpl::physically_connected(ph, safe); }
    int get_connections(PortHandle ph, std::vector<std::string>& r, bool safe) override { return PortEngineSharedImpl::get_connections(ph, r, safe); }

    XMLNode* get_state() const override;
    int set_state(XMLNode const&, int version) override;

    int midi_event_get(pframes_t&, size_t&, uint8_t const**, void*, uint32_t) override;
    int midi_event_put(void*, pframes_t, const uint8_t*, size_t) override;
    uint32_t get_midi_event_count(void*) override;
    void midi_clear(void*) override;

    bool can_monitor_input() const override { return false; }
    int request_input_monitoring(PortHandle, bool) override { return -1; }
    int ensure_input_monitoring(PortHandle, bool) override { return -1; }
    bool monitoring_input(PortHandle) override { return false; }

    void set_latency_range(PortHandle, bool for_playback, LatencyRange) override;
    LatencyRange get_latency_range(PortHandle, bool for_playback) override;
    void* get_buffer(PortHandle, pframes_t) override;

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream*, void*, int32_t) override;
    void onErrorAfterClose(oboe::AudioStream*, oboe::Result) override;

private:
    static constexpr uint32_t kMaxBufferSize = 8192;

    bool open_output_stream(oboe::SharingMode);
    int register_system_audio_ports();
    void process_port_connection_changes();
    bool process_cycle(float* interleaved_output, uint32_t nframes);

    static void* process_thread_entry(void*);

    struct ThreadData {
        std::function<void()> func;
    };

    BackendPort* port_factory(std::string const&, DataType, PortFlags) override;

    std::string _instance_name;
    std::string _device{"Android default"};

    std::shared_ptr<oboe::AudioStream> _output_stream;

    std::atomic<bool> _running{false};
    std::atomic<bool> _active{false};
    std::atomic<bool> _freewheel_requested{false};
    bool _freewheel_active{false};

    float _sample_rate{48000.f};
    uint32_t _frames_per_callback{256};
    uint32_t _output_channels{2};

    uint32_t _systemic_input_latency{0};
    uint32_t _systemic_output_latency{0};

    std::atomic<float> _dsp_load{0.f};
    DSPLoadCalculator _dsp_load_calc;

    std::atomic<samplepos_t> _processed_samples{0};
    std::atomic<int64_t> _cycle_start_us{0};

    pthread_t _main_thread{};
    std::atomic<bool> _main_thread_valid{false};
    std::vector<pthread_t> _threads;
};

} // namespace ARDOUR

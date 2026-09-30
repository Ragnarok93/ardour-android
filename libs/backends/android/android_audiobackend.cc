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
{
    if (data && size > 0) {
        _data.assign(data, data + size);
    }
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
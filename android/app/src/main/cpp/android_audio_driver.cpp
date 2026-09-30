#include "android_audio_driver.h"

#include <cstring>
#include <sstream>

namespace ardour::android {

AndroidAudioDriver::~AndroidAudioDriver() {
    stop();
}

bool AndroidAudioDriver::open(oboe::SharingMode sharingMode) {
    oboe::AudioStreamBuilder builder;

    builder
        .setDirection(oboe::Direction::Output)
        .setPerformanceMode(oboe::PerformanceMode::LowLatency)
        .setSharingMode(sharingMode)
        .setFormat(oboe::AudioFormat::Float)
        .setChannelCount(2)
        .setDataCallback(this);

    std::shared_ptr<oboe::AudioStream> stream;
    const oboe::Result result = builder.openStream(stream);
    if (result != oboe::Result::OK || !stream) {
        return false;
    }

    sample_rate_.store(stream->getSampleRate(), std::memory_order_relaxed);
    channel_count_.store(stream->getChannelCount(), std::memory_order_relaxed);
    frames_per_burst_.store(stream->getFramesPerBurst(), std::memory_order_relaxed);
    exclusive_.store(
        sharingMode == oboe::SharingMode::Exclusive,
        std::memory_order_relaxed
    );

    stream_ = std::move(stream);
    return true;
}

bool AndroidAudioDriver::start() {
    std::scoped_lock lock(control_mutex_);

    if (running_.load(std::memory_order_acquire)) {
        return true;
    }

    stream_.reset();

    // Exclusive gives the best chance of a low-latency path. Android devices
    // are allowed to reject it, so shared mode is the required fallback.
    if (!open(oboe::SharingMode::Exclusive) &&
        !open(oboe::SharingMode::Shared)) {
        sample_rate_.store(0, std::memory_order_relaxed);
        channel_count_.store(0, std::memory_order_relaxed);
        frames_per_burst_.store(0, std::memory_order_relaxed);
        return false;
    }

    const oboe::Result result = stream_->requestStart();
    if (result != oboe::Result::OK) {
        stream_->close();
        stream_.reset();
        return false;
    }

    running_.store(true, std::memory_order_release);
    return true;
}

void AndroidAudioDriver::stop() {
    std::scoped_lock lock(control_mutex_);

    running_.store(false, std::memory_order_release);

    if (!stream_) {
        return;
    }

    stream_->requestStop();
    stream_->close();
    stream_.reset();
}

std::string AndroidAudioDriver::statusText() const {
    std::ostringstream status;
    status << (running() ? "running" : "stopped");

    const int32_t rate = sampleRate();
    if (rate > 0) {
        status << " • " << rate << " Hz";
        status << " • " << channelCount() << " ch";
        status << " • burst " << framesPerBurst();
        status << " • "
               << (exclusive_.load(std::memory_order_relaxed)
                       ? "exclusive"
                       : "shared");
    } else {
        status << " • Oboe ready";
    }

    return status.str();
}

oboe::DataCallbackResult AndroidAudioDriver::onAudioReady(
    oboe::AudioStream* audioStream,
    void* audioData,
    int32_t numFrames
) {
    // Realtime contract:
    // - no allocation
    // - no JNI
    // - no mutex
    // - no filesystem/network calls
    //
    // Bootstrap behavior is silence. libardour processing will replace this
    // write once the AudioEngine and AndroidAudioBackend are linked.
    const int32_t channels = audioStream != nullptr
        ? audioStream->getChannelCount()
        : channel_count_.load(std::memory_order_relaxed);

    if (audioData != nullptr && channels > 0 && numFrames > 0) {
        std::memset(
            audioData,
            0,
            static_cast<size_t>(numFrames) *
                static_cast<size_t>(channels) *
                sizeof(float)
        );
    }

    return running_.load(std::memory_order_relaxed)
        ? oboe::DataCallbackResult::Continue
        : oboe::DataCallbackResult::Stop;
}

AndroidAudioDriver& audioDriver() {
    static AndroidAudioDriver driver;
    return driver;
}

} // namespace ardour::android

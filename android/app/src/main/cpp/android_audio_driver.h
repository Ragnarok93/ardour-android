#pragma once

#include <atomic>
#include <memory>
#include <mutex>
#include <string>

#include <oboe/Oboe.h>

namespace ardour::android {

/**
 * Bootstrap realtime driver used before libardour is linked.
 *
 * The Oboe data callback is intentionally allocation-free and lock-free.
 * Today it renders silence. The next engine milestone will replace that memset
 * with the Ardour AudioEngine process callback while preserving this thread
 * boundary.
 */
class AndroidAudioDriver final : public oboe::AudioStreamDataCallback {
public:
    AndroidAudioDriver() = default;
    ~AndroidAudioDriver() override;

    AndroidAudioDriver(const AndroidAudioDriver&) = delete;
    AndroidAudioDriver& operator=(const AndroidAudioDriver&) = delete;

    bool start();
    void stop();

    bool running() const noexcept {
        return running_.load(std::memory_order_acquire);
    }

    int32_t sampleRate() const noexcept {
        return sample_rate_.load(std::memory_order_relaxed);
    }

    int32_t channelCount() const noexcept {
        return channel_count_.load(std::memory_order_relaxed);
    }

    int32_t framesPerBurst() const noexcept {
        return frames_per_burst_.load(std::memory_order_relaxed);
    }

    std::string statusText() const;

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames
    ) override;

private:
    bool open(oboe::SharingMode sharingMode);

    mutable std::mutex control_mutex_;
    std::shared_ptr<oboe::AudioStream> stream_;

    std::atomic<bool> running_{false};
    std::atomic<int32_t> sample_rate_{0};
    std::atomic<int32_t> channel_count_{0};
    std::atomic<int32_t> frames_per_burst_{0};
    std::atomic<bool> exclusive_{false};
};

AndroidAudioDriver& audioDriver();

} // namespace ardour::android

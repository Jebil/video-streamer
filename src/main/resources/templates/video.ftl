<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${videoPath} - Video Streamer</title>
    <style>
        :root {
            --bg: #0b0f19;
            --surface: #151d2e;
            --surface-hover: #1e293b;
            --primary: #38bdf8;
            --text: #f8fafc;
            --text-muted: #94a3b8;
            --border: #334155;
        }
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background-color: var(--bg);
            color: var(--text);
            padding: 1.5rem 1rem;
            min-height: 100vh;
        }
        .container {
            max-width: 1080px;
            margin: 0 auto;
        }
        .nav-back {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            color: var(--primary);
            text-decoration: none;
            font-weight: 500;
            font-size: 0.95rem;
            margin-bottom: 1.25rem;
            padding: 0.45rem 0.9rem;
            border-radius: 0.5rem;
            background: var(--surface);
            border: 1px solid var(--border);
            transition: all 0.2s;
        }
        .nav-back:hover {
            background: var(--surface-hover);
            border-color: var(--primary);
        }
        .player-wrapper {
            position: relative;
            background-color: #000;
            border-radius: 0.75rem;
            overflow: hidden;
            box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.7);
            border: 1px solid var(--border);
        }
        video {
            width: 100%;
            max-height: 78vh;
            display: block;
            outline: none;
        }
        .toast {
            position: absolute;
            top: 1rem;
            right: 1rem;
            background: rgba(15, 23, 42, 0.85);
            backdrop-filter: blur(8px);
            color: var(--text);
            border: 1px solid var(--border);
            padding: 0.5rem 1rem;
            border-radius: 0.5rem;
            font-size: 0.875rem;
            pointer-events: none;
            opacity: 0;
            transition: opacity 0.2s ease-in-out;
            z-index: 10;
        }
        .toast.show {
            opacity: 1;
        }
        .video-meta {
            margin-top: 1.5rem;
            padding: 1.25rem;
            background: var(--surface);
            border-radius: 0.75rem;
            border: 1px solid var(--border);
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 1rem;
        }
        .video-title {
            font-size: 1.2rem;
            font-weight: 600;
            word-break: break-all;
        }
        .actions {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }
        .btn {
            font-size: 0.875rem;
            padding: 0.45rem 0.9rem;
            border-radius: 0.5rem;
            text-decoration: none;
            color: var(--text);
            background: var(--surface-hover);
            border: 1px solid var(--border);
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            gap: 0.4rem;
        }
        .btn:hover {
            border-color: var(--primary);
            color: var(--primary);
        }
        .shortcuts-guide {
            margin-top: 1.5rem;
            padding: 1rem 1.25rem;
            background: rgba(21, 29, 46, 0.5);
            border: 1px solid var(--border);
            border-radius: 0.75rem;
            font-size: 0.85rem;
            color: var(--text-muted);
        }
        .shortcuts-guide strong {
            color: var(--text);
        }
        .shortcuts-grid {
            display: flex;
            flex-wrap: wrap;
            gap: 1.25rem;
            margin-top: 0.5rem;
        }
        kbd {
            background: #1e293b;
            color: var(--primary);
            padding: 0.15rem 0.4rem;
            border-radius: 0.25rem;
            border: 1px solid #475569;
            font-family: inherit;
            font-size: 0.8rem;
        }
    </style>
</head>
<body>
    <div class="container">
        <a href="/" class="nav-back">← Back to video list</a>

        <div class="player-wrapper">
            <video id="player" controls autoplay preload="metadata">
                <source src="/videos/${videoPath}">
                Your browser does not support HTML5 video playback.
            </video>
            <div id="toast" class="toast"></div>
        </div>

        <div class="video-meta">
            <div>
                <div class="video-title">${videoPath}</div>
            </div>
            <div class="actions">
                <a href="/videos/full/${videoPath}" class="btn" download>📥 Download Full Video</a>
            </div>
        </div>

        <div class="shortcuts-guide">
            <strong>Keyboard Controls:</strong>
            <div class="shortcuts-grid">
                <span><kbd>Space</kbd> Play / Pause</span>
                <span><kbd>←</kbd> <kbd>→</kbd> Seek 5s</span>
                <span><kbd>↑</kbd> <kbd>↓</kbd> Volume</span>
                <span><kbd>F</kbd> Fullscreen</span>
                <span><kbd>M</kbd> Mute</span>
                <span><kbd>[</kbd> <kbd>]</kbd> Playback Speed</span>
            </div>
        </div>
    </div>

    <script>
        const video = document.getElementById('player');
        const toast = document.getElementById('toast');
        const storageKey = 'video_pos_' + encodeURIComponent('${videoPath}');
        let toastTimeout;

        function showToast(message) {
            if (!toast) return;
            toast.textContent = message;
            toast.classList.add('show');
            clearTimeout(toastTimeout);
            toastTimeout = setTimeout(() => toast.classList.remove('show'), 2000);
        }

        // Auto-resume from localStorage
        const savedTime = localStorage.getItem(storageKey);
        if (savedTime && parseFloat(savedTime) > 5) {
            video.addEventListener('loadedmetadata', () => {
                const target = parseFloat(savedTime);
                if (target < video.duration - 10) {
                    video.currentTime = target;
                    const mins = Math.floor(target / 60);
                    const secs = Math.floor(target % 60);
                    showToast('Resumed at ' + mins + ':' + (secs < 10 ? '0' : '') + secs);
                }
            }, { once: true });
        }

        // Save position every 3 seconds
        video.addEventListener('timeupdate', () => {
            if (video.currentTime > 5 && !video.ended) {
                localStorage.setItem(storageKey, video.currentTime);
            }
        });

        // Reset position on video end
        video.addEventListener('ended', () => {
            localStorage.removeItem(storageKey);
        });

        // Keyboard Shortcuts
        window.addEventListener('keydown', (e) => {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

            switch (e.code) {
                case 'Space':
                    e.preventDefault();
                    if (video.paused) {
                        video.play();
                        showToast('▶ Play');
                    } else {
                        video.pause();
                        showToast('⏸ Pause');
                    }
                    break;
                case 'ArrowLeft':
                    e.preventDefault();
                    video.currentTime = Math.max(0, video.currentTime - 5);
                    showToast('⏪ -5s');
                    break;
                case 'ArrowRight':
                    e.preventDefault();
                    video.currentTime = Math.min(video.duration, video.currentTime + 5);
                    showToast('⏩ +5s');
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    video.volume = Math.min(1, video.volume + 0.1);
                    showToast('🔊 Volume: ' + Math.round(video.volume * 100) + '%');
                    break;
                case 'ArrowDown':
                    e.preventDefault();
                    video.volume = Math.max(0, video.volume - 0.1);
                    showToast('🔉 Volume: ' + Math.round(video.volume * 100) + '%');
                    break;
                case 'KeyF':
                    e.preventDefault();
                    if (!document.fullscreenElement) {
                        video.requestFullscreen().catch(() => {});
                    } else {
                        document.exitFullscreen().catch(() => {});
                    }
                    break;
                case 'KeyM':
                    e.preventDefault();
                    video.muted = !video.muted;
                    showToast(video.muted ? '🔇 Muted' : '🔊 Unmuted');
                    break;
                case 'BracketRight':
                    e.preventDefault();
                    video.playbackRate = Math.min(2.5, +(video.playbackRate + 0.25).toFixed(2));
                    showToast('⚡ Speed: ' + video.playbackRate + 'x');
                    break;
                case 'BracketLeft':
                    e.preventDefault();
                    video.playbackRate = Math.max(0.25, +(video.playbackRate - 0.25).toFixed(2));
                    showToast('⚡ Speed: ' + video.playbackRate + 'x');
                    break;
            }
        });
    </script>
</body>
</html>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Video Streamer</title>
    <style>
        :root {
            --bg: #0b0f19;
            --surface: #151d2e;
            --surface-hover: #1e293b;
            --primary: #38bdf8;
            --primary-hover: #0ea5e9;
            --text: #f8fafc;
            --text-muted: #94a3b8;
            --border: #334155;
            --tag-bg: #1e293b;
            --tag-border: #475569;
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
            padding: 2rem 1rem;
            min-height: 100vh;
        }
        .container {
            max-width: 960px;
            margin: 0 auto;
        }
        header {
            margin-bottom: 2rem;
            border-bottom: 1px solid var(--border);
            padding-bottom: 1.5rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 1rem;
        }
        .header-title {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }
        h1 {
            font-size: 1.75rem;
            font-weight: 700;
        }
        .header-actions {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }
        .badge {
            background: var(--surface);
            color: var(--primary);
            padding: 0.35rem 0.85rem;
            border-radius: 9999px;
            font-size: 0.875rem;
            font-weight: 600;
            border: 1px solid var(--border);
        }
        .btn-refresh {
            background: var(--surface);
            color: var(--text);
            border: 1px solid var(--border);
            padding: 0.35rem 0.85rem;
            border-radius: 0.5rem;
            font-size: 0.875rem;
            cursor: pointer;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 0.4rem;
            transition: all 0.2s;
        }
        .btn-refresh:hover {
            background: var(--surface-hover);
            border-color: var(--primary);
            color: var(--primary);
        }
        .search-bar {
            margin-bottom: 1.5rem;
            position: relative;
        }
        .search-input {
            width: 100%;
            background: var(--surface);
            border: 1px solid var(--border);
            color: var(--text);
            padding: 0.75rem 1rem 0.75rem 2.5rem;
            border-radius: 0.75rem;
            font-size: 1rem;
            outline: none;
            transition: border-color 0.2s;
        }
        .search-input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.15);
        }
        .search-icon {
            position: absolute;
            left: 0.85rem;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            pointer-events: none;
        }
        .video-grid {
            display: grid;
            gap: 0.85rem;
        }
        .video-card {
            background-color: var(--surface);
            border: 1px solid var(--border);
            border-radius: 0.75rem;
            padding: 1rem 1.25rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            text-decoration: none;
            color: inherit;
            transition: all 0.2s ease;
        }
        .video-card:hover {
            background-color: var(--surface-hover);
            border-color: var(--primary);
            transform: translateY(-2px);
        }
        .video-main {
            display: flex;
            align-items: center;
            gap: 1rem;
            min-width: 0;
        }
        .play-icon {
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: rgba(56, 189, 248, 0.12);
            color: var(--primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.15rem;
            flex-shrink: 0;
            transition: background 0.2s, transform 0.2s;
        }
        .video-card:hover .play-icon {
            background: var(--primary);
            color: #0b0f19;
            transform: scale(1.08);
        }
        .video-details {
            min-width: 0;
        }
        .video-title {
            font-weight: 600;
            font-size: 1.05rem;
            color: var(--text);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .video-path {
            font-size: 0.825rem;
            color: var(--text-muted);
            margin-top: 0.2rem;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .video-meta {
            display: flex;
            align-items: center;
            gap: 0.6rem;
            flex-shrink: 0;
            margin-left: 1rem;
        }
        .ext-tag {
            background: var(--tag-bg);
            border: 1px solid var(--tag-border);
            color: var(--primary);
            font-size: 0.75rem;
            font-weight: 700;
            padding: 0.2rem 0.5rem;
            border-radius: 0.375rem;
            letter-spacing: 0.05em;
        }
        .size-tag {
            font-size: 0.8rem;
            color: var(--text-muted);
        }
        .empty-state {
            text-align: center;
            padding: 3.5rem 1rem;
            background: var(--surface);
            border-radius: 0.75rem;
            border: 1px dashed var(--border);
        }
        .empty-state h3 {
            font-size: 1.25rem;
            margin-bottom: 0.5rem;
        }
        .empty-state p {
            color: var(--text-muted);
            font-size: 0.95rem;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div class="header-title">
                <h1>🎬 Video Streamer</h1>
                <span class="badge" id="videoCounter">${videos?size} videos</span>
            </div>
            <div class="header-actions">
                <a href="/playlist.m3u" class="btn-refresh" title="Open entire library in VLC (M3U Playlist)">📺 VLC Playlist</a>
                <a href="/refresh" class="btn-refresh" title="Rescan video folder">🔄 Refresh Library</a>
            </div>
        </header>

        <#if videos?has_content>
            <div class="search-bar">
                <span class="search-icon">🔍</span>
                <input type="text" id="searchInput" class="search-input" placeholder="Quick search by title or subfolder...">
            </div>

            <div class="video-grid" id="videoGrid">
                <#list videos as video>
                    <a href="/watch/${video.path}" class="video-card" data-title="${video.title?lower_case}" data-path="${video.path?lower_case}">
                        <div class="video-main">
                            <div class="play-icon">▶</div>
                            <div class="video-details">
                                <div class="video-title">${video.title}</div>
                                <div class="video-path">${video.path}</div>
                            </div>
                        </div>
                        <div class="video-meta">
                            <span class="ext-tag">${video.extension!"VIDEO"}</span>
                            <#if video.formattedSize?? && video.formattedSize != "0 B">
                                <span class="size-tag">${video.formattedSize}</span>
                            </#if>
                        </div>
                    </a>
                </#list>
            </div>
            <div id="noMatchState" class="empty-state" style="display: none; margin-top: 1rem;">
                <h3>No matching videos found</h3>
                <p>Try searching with a different keyword.</p>
            </div>
        <#else>
            <div class="empty-state">
                <h3>No video files found</h3>
                <p>Configure the <code>MOVIES_PATH</code> environment variable or <code>video.location</code> in <code>application.yml</code> to point to a folder containing supported video files (.mp4, .mkv, .avi, .webm, etc.).</p>
            </div>
        </#if>
    </div>

    <script>
        const searchInput = document.getElementById('searchInput');
        const videoGrid = document.getElementById('videoGrid');
        const noMatch = document.getElementById('noMatchState');
        const counter = document.getElementById('videoCounter');

        if (searchInput && videoGrid) {
            const cards = Array.from(videoGrid.querySelectorAll('.video-card'));
            const totalCount = cards.length;

            searchInput.addEventListener('input', (e) => {
                const query = e.target.value.trim().toLowerCase();
                let visibleCount = 0;

                cards.forEach(card => {
                    const title = card.getAttribute('data-title') || '';
                    const path = card.getAttribute('data-path') || '';
                    if (title.includes(query) || path.includes(query)) {
                        card.style.display = 'flex';
                        visibleCount++;
                    } else {
                        card.style.display = 'none';
                    }
                });

                if (noMatch) {
                    noMatch.style.display = visibleCount === 0 ? 'block' : 'none';
                }

                if (counter) {
                    counter.textContent = query ? visibleCount + ' / ' + totalCount + ' videos' : totalCount + ' videos';
                }
            });
        }
    </script>
</body>
</html>

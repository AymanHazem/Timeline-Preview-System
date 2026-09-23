const uploadForm = document.querySelector('#uploadForm');
const videoInput = document.querySelector('#videoInput');
const dropZone = document.querySelector('#dropZone');
const fileRow = document.querySelector('#fileRow');
const fileName = document.querySelector('#fileName');
const fileSize = document.querySelector('#fileSize');
const changeFileButton = document.querySelector('#changeFileButton');
const generateButton = document.querySelector('#generateButton');
const buttonLabel = generateButton.querySelector('.button-label');
const statusPill = document.querySelector('#statusPill');
const statusText = document.querySelector('#statusText');
const message = document.querySelector('#message');
const playerCard = document.querySelector('#playerCard');
const videoPlayer = document.querySelector('#videoPlayer');
const videoFrame = document.querySelector('.video-frame');
const playButton = document.querySelector('#playButton');
const seekBar = document.querySelector('#seekBar');
const currentTimeLabel = document.querySelector('#currentTime');
const durationLabel = document.querySelector('#duration');
const timeline = document.querySelector('#timeline');
const timelineHint = document.querySelector('#timelineHint');
const thumbnailPreview = document.querySelector('#thumbnailPreview');
const thumbnailImage = document.querySelector('#thumbnailImage');
const thumbnailTime = document.querySelector('#thumbnailTime');

let selectedFile = null;
let videoObjectUrl = null;
let cues = [];
let spriteUrl = '';

const setStatus = (text, state = 'idle') => {
    statusText.textContent = text;
    statusPill.dataset.state = state;
};

const formatBytes = (bytes) => {
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const formatTime = (seconds) => {
    if (!Number.isFinite(seconds)) return '00:00';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    const shortTime = `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
    return hours > 0 ? `${String(hours).padStart(2, '0')}:${shortTime}` : shortTime;
};

const useFile = (file) => {
    if (!file || !file.type.startsWith('video/')) {
        showError('Please choose a valid video file.');
        return;
    }

    selectedFile = file;
    cues = [];
    spriteUrl = '';
    message.hidden = true;
    dropZone.hidden = true;
    fileRow.hidden = false;
    fileName.textContent = file.name;
    fileSize.textContent = formatBytes(file.size);
    generateButton.disabled = false;
    timelineHint.textContent = 'Generate previews, then hover over the timeline.';
    setStatus('Ready to process');

    if (videoObjectUrl) URL.revokeObjectURL(videoObjectUrl);
    videoObjectUrl = URL.createObjectURL(file);
    playerCard.classList.remove('is-portrait');
    videoFrame.style.removeProperty('aspect-ratio');
    videoPlayer.src = videoObjectUrl;
    playerCard.hidden = false;
};

const showError = (text) => {
    message.textContent = text;
    message.hidden = false;
    setStatus('Processing failed', 'error');
};

const setLoading = (loading) => {
    generateButton.disabled = loading;
    generateButton.classList.toggle('is-loading', loading);
    buttonLabel.textContent = loading ? 'Processing video…' : 'Generate timeline preview';
};

videoInput.addEventListener('change', () => useFile(videoInput.files[0]));
changeFileButton.addEventListener('click', () => videoInput.click());

for (const eventName of ['dragenter', 'dragover']) {
    dropZone.addEventListener(eventName, (event) => {
        event.preventDefault();
        dropZone.classList.add('is-dragging');
    });
}
for (const eventName of ['dragleave', 'drop']) {
    dropZone.addEventListener(eventName, (event) => {
        event.preventDefault();
        dropZone.classList.remove('is-dragging');
    });
}
dropZone.addEventListener('drop', (event) => useFile(event.dataTransfer.files[0]));

const parseTimestamp = (timestamp) => {
    const parts = timestamp.split(':').map(Number);
    return parts[0] * 3600 + parts[1] * 60 + parts[2];
};

const parseVtt = (content) => {
    const cuePattern = /(\d{2}:\d{2}:\d{2}\.\d{3})\s+-->\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s*\n([^\n]+)#xywh=(\d+),(\d+),(\d+),(\d+)/g;
    return Array.from(content.matchAll(cuePattern), (match) => ({
        start: parseTimestamp(match[1]),
        end: parseTimestamp(match[2]),
        x: Number(match[4]),
        y: Number(match[5]),
        width: Number(match[6]),
        height: Number(match[7])
    }));
};

uploadForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!selectedFile) return;

    setLoading(true);
    setStatus('Generating thumbnails', 'working');
    message.hidden = true;

    try {
        const formData = new FormData();
        formData.append('file', selectedFile);
        const response = await fetch('/api/videos/upload', { method: 'POST', body: formData });
        const result = await response.json();
        if (!response.ok) throw new Error(result.error || 'The upload could not be processed.');

        const vttResponse = await fetch(result.vttURL);
        if (!vttResponse.ok) throw new Error('The server did not generate timeline previews. Check that FFmpeg is installed.');

        cues = parseVtt(await vttResponse.text());
        if (cues.length === 0) throw new Error('No preview frames were generated for this video.');

        spriteUrl = result.spriteURL;
        const spriteCheck = await fetch(spriteUrl);
        if (!spriteCheck.ok) throw new Error('The generated sprite sheet could not be loaded.');

        timelineHint.textContent = `${cues.length} thumbnails generated · hover over the timeline to preview`;
        setStatus('Preview ready', 'ready');
    } catch (error) {
        showError(error.message || 'Something went wrong while processing the video.');
    } finally {
        setLoading(false);
    }
});

const togglePlayback = () => videoPlayer.paused ? videoPlayer.play() : videoPlayer.pause();
playButton.addEventListener('click', togglePlayback);
videoPlayer.addEventListener('click', togglePlayback);
videoPlayer.addEventListener('play', () => videoFrame.classList.add('is-playing'));
videoPlayer.addEventListener('pause', () => videoFrame.classList.remove('is-playing'));
videoPlayer.addEventListener('ended', () => videoFrame.classList.remove('is-playing'));
videoPlayer.addEventListener('loadedmetadata', () => {
    durationLabel.textContent = formatTime(videoPlayer.duration);

    if (videoPlayer.videoWidth > 0 && videoPlayer.videoHeight > 0) {
        videoFrame.style.aspectRatio = `${videoPlayer.videoWidth} / ${videoPlayer.videoHeight}`;
        playerCard.classList.toggle('is-portrait', videoPlayer.videoHeight > videoPlayer.videoWidth);
    }
});
videoPlayer.addEventListener('timeupdate', () => {
    currentTimeLabel.textContent = formatTime(videoPlayer.currentTime);
    const progress = videoPlayer.duration ? videoPlayer.currentTime / videoPlayer.duration : 0;
    seekBar.value = String(progress * 1000);
    seekBar.style.setProperty('--progress', `${progress * 100}%`);
});
seekBar.addEventListener('input', () => {
    if (!videoPlayer.duration) return;
    videoPlayer.currentTime = (Number(seekBar.value) / 1000) * videoPlayer.duration;
});

const showThumbnail = (event) => {
    if (cues.length === 0 || !videoPlayer.duration) return;

    const bounds = timeline.getBoundingClientRect();
    const pointerX = Math.max(0, Math.min(event.clientX - bounds.left, bounds.width));
    const hoverTime = (pointerX / bounds.width) * videoPlayer.duration;
    const cue = cues.find((item) => hoverTime >= item.start && hoverTime < item.end) || cues[cues.length - 1];

    thumbnailImage.style.width = `${cue.width}px`;
    thumbnailImage.style.height = `${cue.height}px`;
    thumbnailImage.style.backgroundImage = `url("${spriteUrl}")`;
    thumbnailImage.style.backgroundPosition = `-${cue.x}px -${cue.y}px`;
    thumbnailTime.textContent = formatTime(hoverTime);

    const halfPreview = cue.width / 2;
    const clampedX = Math.max(halfPreview, Math.min(pointerX, bounds.width - halfPreview));
    thumbnailPreview.style.left = `${clampedX}px`;
    thumbnailPreview.classList.add('is-visible');
};

timeline.addEventListener('pointermove', showThumbnail);
timeline.addEventListener('pointerleave', () => thumbnailPreview.classList.remove('is-visible'));
window.addEventListener('beforeunload', () => {
    if (videoObjectUrl) URL.revokeObjectURL(videoObjectUrl);
});

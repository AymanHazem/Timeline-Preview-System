# Timeline Preview System

A Spring Boot application that generates YouTube-style timeline previews from uploaded videos. It uses FFmpeg to extract frames, combines those frames into a JPEG sprite sheet, and creates a WebVTT file that maps playback timestamps to thumbnail coordinates.

The project includes a responsive browser demo where users can upload a video, play it locally, and hover over the timeline to view the generated thumbnails.

## Features

- Upload videos from a browser
- Extract frames at configurable intervals with FFmpeg
- Generate compact JPEG sprite sheets
- Generate WebVTT timeline metadata
- Preview thumbnails while hovering over the video timeline
- Adapt the video player to portrait, landscape, and square videos
- Run locally with Maven or as a Docker container
- Deploy to Render using the included Blueprint

## Tech Stack

- Java 25
- Spring Boot 4.1.1
- Spring MVC
- FFmpeg
- Maven
- HTML, CSS, and vanilla JavaScript
- Docker

## How It Works

```text
Video upload
    ↓
FFmpeg extracts frames at fixed intervals
    ↓
SpriteService combines frames into a JPEG sprite sheet
    ↓
VTTService maps timestamps to sprite coordinates
    ↓
The frontend loads the WebVTT data and displays thumbnails on hover
```

Generated files are stored under:

```text
uploads/                 Temporary uploaded videos
outputs/{videoId}/       Generated sprite and WebVTT files
```

## Requirements

For local development:

- Java 25
- FFmpeg available on `PATH`
- No separate Maven installation is required; the project includes Maven Wrapper scripts

Verify the required tools:

```bash
java --version
ffmpeg -version
```

## Run Locally

Start the application:

```bash
sh mvnw spring-boot:run
```

Open <http://localhost:8080>.

The default maximum upload size is 100 MB. Keep demo videos reasonably short because processing happens synchronously during the upload request.

## Run Tests

```bash
sh mvnw test
```

## Run with Docker

Docker packages Java, the application, and FFmpeg into one image.

```bash
docker build -t timeline-preview-system .
docker run --rm -p 8080:8080 timeline-preview-system
```

Open <http://localhost:8080>.

## API

### Upload and process a video

```http
POST /api/videos/upload
Content-Type: multipart/form-data
```

Form field:

| Field | Type | Description |
| --- | --- | --- |
| `file` | Video file | Video to process |

Example:

```bash
curl -X POST \
  -F "file=@sample.mp4" \
  http://localhost:8080/api/videos/upload
```

Example response:

```json
{
  "videoID": "d703611d-b524-4ff7-84e6-65ccf78b67de",
  "spriteURL": "/api/videos/d703611d-b524-4ff7-84e6-65ccf78b67de/sprite",
  "vttURL": "/api/videos/d703611d-b524-4ff7-84e6-65ccf78b67de/vtt",
  "message": "Good"
}
```

### Get a sprite sheet

```http
GET /api/videos/{videoID}/sprite
```

Returns the generated JPEG sprite sheet.

### Get WebVTT metadata

```http
GET /api/videos/{videoID}/vtt
```

Returns the generated `text/vtt` timeline metadata.

## Configuration

Configuration lives in `src/main/resources/application.properties` and can be overridden with environment variables.

| Environment variable | Default | Description |
| --- | --- | --- |
| `PORT` | `8080` | HTTP server port |
| `UPLOAD_DIR` | `uploads` | Uploaded-video directory |
| `OUTPUT_DIR` | `outputs` | Generated-output directory |
| `MAX_UPLOAD_SIZE` | `100MB` | Maximum multipart upload size |
| `FRAME_INTERVAL` | `5` | Seconds between extracted frames |

A local `.env` file can also provide Spring properties. The file is optional and excluded from Git.

## Deploy on Render

The repository includes `render.yaml` and a production-ready `Dockerfile`.

1. Push the repository to GitHub.
2. Sign in to [Render](https://render.com).
3. Select **New → Blueprint**.
4. Connect this repository.
5. Apply the detected `render.yaml` Blueprint.
6. Wait for the Docker build and open the assigned URL.

See [`DEPLOYMENT.md`](DEPLOYMENT.md) for detailed instructions and Docker-hosting options.

## Project Structure

```text
src/main/java/com/ayman/TimelinePreviewSystem/
├── Controller/
│   └── VideoController.java
├── Service/
│   ├── FfmpegService.java
│   ├── SpriteService.java
│   ├── VTTService.java
│   └── VideoProcessingService.java
└── TimelinePreviewSystemApplication.java

src/main/resources/
├── static/
│   ├── app.js
│   ├── index.html
│   └── styles.css
└── application.properties
```

## Current Limitations

- Processing is synchronous, so large videos can exceed free-hosting request timeouts.
- Uploaded videos and generated previews are stored on the local filesystem.
- Free Render storage is ephemeral; files disappear after a restart or redeployment.
- The application currently has no authentication or upload rate limiting and is intended as a demonstration project.

For persistent production storage, move uploads and generated files to an object-storage service such as Cloudflare R2, Amazon S3, or another S3-compatible provider.

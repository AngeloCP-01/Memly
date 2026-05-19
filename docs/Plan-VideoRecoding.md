Plan: Add Video Recording via System Camera

Context

The PRD lists video capture as a core feature, but
the camera button only launches TakePicture (photos).
Videos can be picked from the gallery but not
recorded in-app. The entire downstream pipeline
(MediaStore insert, thumbnails, dedup, playback)
already handles MediaType.VIDEO — only the camera
launch flow is missing.

Approach

Single "Camera" button → AlertDialog asking "Photo"
or "Video" → launches the appropriate system camera
intent. Two files get the camera changes; both
ViewModels get a mediaType parameter added.

Changes

1. CaptureViewModel.kt (1 line)

- Add mediaType: MediaType = MediaType.PHOTO
  parameter to addCameraMedia()

2. MemoryDetailViewModel.kt (1 line)

- Same change to its addCameraMedia() at line 244

3. CaptureScreen.kt (main changes)

- Add cameraVideoUri state variable
- Add CaptureVideo() contract launcher (mirrors
  existing TakePicture launcher)
- Add showCameraModeDialog state variable
- Modify camera permission callback: on granted →
  show dialog instead of directly launching photo
- Add AlertDialog with "Photo" / "Video" buttons:
    - Photo: creates .jpg temp file in
      cacheDir/camera/, launches cameraLauncher
    - Video: creates .mp4 temp file in
      cacheDir/camera/, launches videoLauncher

4. MemoryDetailScreen.kt (same pattern for edit mode)

- Add editCameraVideoUri state variable
- Add CaptureVideo() contract launcher
- Add showEditCameraModeDialog state variable
- Modify edit camera permission callback to show
  dialog
- Add same Photo/Video AlertDialog

No changes needed

- file_paths.xml — memly_camera path already covers
  cacheDir/camera/ for both .jpg and .mp4
- MediaStoreManager — already handles VIDEO insertion
- ThumbnailUtil — already extracts video frames
- processMediaItem() — already handles VIDEO with
  isFromCamera = true

Verification

1. Build with ./gradlew assembleDebug
2. Test: Camera button → dialog appears → "Photo" →
   system camera opens in photo mode → photo saved
3. Test: Camera button → dialog appears → "Video" →
   system camera opens in video mode → video saved,
   thumbnail shown, plays in detail view
4. Test: Same flow from edit mode in
   MemoryDetailScreen
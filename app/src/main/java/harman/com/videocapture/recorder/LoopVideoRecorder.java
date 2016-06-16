package harman.com.videocapture.recorder;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import harman.com.videocapture.CLog;
import harman.com.videocapture.VideoFile;
import harman.com.videocapture.camera.CameraWrapper;
import harman.com.videocapture.camera.OpenCameraException;
import harman.com.videocapture.camera.PrepareCameraException;
import harman.com.videocapture.camera.RecordingSize;
import harman.com.videocapture.configuration.CaptureConfiguration;
import harman.com.videocapture.preview.CapturePreview;
import harman.com.videocapture.preview.CapturePreviewInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 */
public class LoopVideoRecorder implements MediaRecorder.OnInfoListener, CapturePreviewInterface {

    private CameraWrapper mCameraWrapper;
    private Surface mPreviewSurface;
    private CapturePreview mVideoCapturePreview = null;

    private final CaptureConfiguration mCaptureConfiguration;
    private ArrayList<VideoFile> mVideoFileList;
    private int mFileIndex;
    public VideoFile mVideoFile;

    private MediaRecorder mRecorder;
    private boolean mRecording = false;
    private final VideoRecorderInterface mRecorderInterface;

    private LoopVideoRecorderListener mListener = null;

    public interface LoopVideoRecorderListener {
        void onLoopRecordingStopped(ArrayList<VideoFile> videoFileList, int fileIndex);
    }

    public LoopVideoRecorder(VideoRecorderInterface recorderInterface, CaptureConfiguration captureConfiguration, ArrayList<VideoFile> videoFileList, int fileIndex,
                         CameraWrapper cameraWrapper, SurfaceHolder previewHolder) {
        mCaptureConfiguration = captureConfiguration;
        mRecorderInterface = recorderInterface;
        mVideoFileList = videoFileList;
        mFileIndex = fileIndex;


        mVideoFile = mVideoFileList.get(mFileIndex);
        mCameraWrapper = cameraWrapper;
        mPreviewSurface = previewHolder.getSurface();

        initializeCamera();
        initializePreview(previewHolder);
    }

    public void setLoopVideoRecorderListener(LoopVideoRecorderListener listener) {
        this.mListener = listener;
    }


    protected void initializeCamera() {
        try {
            mCameraWrapper.openCamera();
        } catch (final OpenCameraException e) {
            e.printStackTrace();
            mRecorderInterface.onRecordingFailed(e.getMessage());
            return;
        }

    }

    public void initializePreview(SurfaceHolder previewHolder) {
        mVideoCapturePreview = new CapturePreview(this, mCameraWrapper, previewHolder);
    }

    public void toggleRecording() {
        if (isRecording()) {
            stopRecording(null);
        } else {
            startRecording();
        }
    }

//    public void startPreview(SurfaceHolder previewHolder){
//        mPreviewSurface = previewHolder.getSurface();
//        initializePreview(previewHolder);
//        if(mRecorder != null) {
//            mRecorder.setPreviewDisplay(mPreviewSurface);
//        }
//    }
//
//    public void stopPreview(){
//        if (!isRecording()) return;
//        if(mRecorder != null) {
//            mRecorder.setPreviewDisplay(null);
//        }
//        releasePreviewResources();
//    }

    protected void startRecording() {
        mRecording = false;

        if (!initRecorder()) return;
        if (!prepareRecorder()) return;
        if (!startRecorder()) return;


        mVideoFile.setData(new Date());
        mVideoFileList.set(mFileIndex, mVideoFile);

        mRecording = true;
        mRecorderInterface.onRecordingStarted();
        //CLog.d(CLog.RECORDER, "Successfully started recording - outputfile: " + mVideoFile.getFullPath());
    }



    public void stopRecording(String message) {
        if (!isRecording()) return;

        if(mRecorder != null) {
            try {
                mRecorder.setOnErrorListener(null);
                mRecorder.setPreviewDisplay(null);
                mRecorder.stop();
//                mRecorder.release();
                if (message == null) {
                    mRecorderInterface.onRecordingSuccess();
                }
                CLog.d(CLog.RECORDER, "Successfully stopped recording - outputfile: " + mVideoFile.getFullPath());
            } catch (final RuntimeException e) {
                CLog.d(CLog.RECORDER, "Failed to stop recording");
            }

            mRecording = false;
            long time = System.currentTimeMillis();
            mVideoFile.setStopDate(new Date(time));
//            mVideoFile.completeFilePath();
            mRecorderInterface.onRecordingStopped(mVideoFile);

            if ((message == null) && (mListener != null)) {
                mListener.onLoopRecordingStopped(mVideoFileList, mFileIndex);
            }
            setNextVideoFile();
        }
    }

    private boolean initRecorder() {
        try {
            mCameraWrapper.prepareCameraForRecording();
        } catch (final PrepareCameraException e) {
            e.printStackTrace();
            mRecorderInterface.onRecordingFailed("Unable to record video");
            CLog.e(CLog.RECORDER, "Failed to initialize recorder - " + e.toString());
            return false;
        }

        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setOnErrorListener(null);
        } else {
            mRecorder.reset();
        }

        configureMediaRecorder(mRecorder, mCameraWrapper.getCamera());

        CLog.d(CLog.RECORDER, "MediaRecorder successfully initialized");
        return true;
    }

    @SuppressWarnings("deprecation")
    protected void configureMediaRecorder(final MediaRecorder recorder, android.hardware.Camera camera) throws IllegalStateException, IllegalArgumentException {
        recorder.setCamera(camera);
        recorder.setAudioSource(mCaptureConfiguration.getAudioSource());
        recorder.setVideoSource(mCaptureConfiguration.getVideoSource());

        CamcorderProfile baseProfile = mCameraWrapper.getBaseRecordingProfile();
        baseProfile.fileFormat = mCaptureConfiguration.getOutputFormat();
        baseProfile.duration = mCaptureConfiguration.getMaxCaptureDuration();

        RecordingSize size = mCameraWrapper.getSupportedRecordingSize(mCaptureConfiguration.getVideoWidth(), mCaptureConfiguration.getVideoHeight());
        baseProfile.videoFrameWidth = size.width;
        baseProfile.videoFrameHeight = size.height;
        baseProfile.videoBitRate = mCaptureConfiguration.getVideoBitrate();

        baseProfile.audioCodec = mCaptureConfiguration.getAudioEncoder();
        baseProfile.videoCodec = mCaptureConfiguration.getVideoEncoder();

        recorder.setProfile(baseProfile);
        recorder.setOutputFile(mVideoFile.getFullPath());

        if(mCaptureConfiguration.getMaxCaptureFileSize() != CaptureConfiguration.NO_FILESIZE_LIMIT) {
            try {
                recorder.setMaxFileSize(mCaptureConfiguration.getMaxCaptureFileSize());
            } catch (IllegalArgumentException e) {
                CLog.e(CLog.RECORDER, "Failed to set max filesize - illegal argument: " + mCaptureConfiguration.getMaxCaptureFileSize());
            } catch (RuntimeException e2) {
                CLog.e(CLog.RECORDER, "Failed to set max filesize - runtime exception");
            }
        }

        // added by xxm, for loop recording mode
        if(mCaptureConfiguration.getMaxCaptureDuration() != CaptureConfiguration.NO_DURATION_LIMIT) {
            try {
                recorder.setMaxDuration(mCaptureConfiguration.getMaxCaptureDuration());
            } catch (IllegalArgumentException e) {
                CLog.e(CLog.RECORDER, "Failed to set max duration - illegal argument: " + mCaptureConfiguration.getMaxCaptureDuration());
            } catch (RuntimeException e2) {
                CLog.e(CLog.RECORDER, "Failed to set max duration - runtime exception");
            }
        }

        recorder.setOnInfoListener(this);
    }

    private boolean prepareRecorder() {
        try {
            mRecorder.prepare();
            CLog.d(CLog.RECORDER, "MediaRecorder successfully prepared");
            return true;
        } catch (final IllegalStateException e) {
            e.printStackTrace();
            CLog.e(CLog.RECORDER, "MediaRecorder preparation failed - " + e.toString());
            return false;
        } catch (final IOException e) {
            e.printStackTrace();
            CLog.e(CLog.RECORDER, "MediaRecorder preparation failed - " + e.toString());
            return false;
        }
    }

    private boolean startRecorder() {
        try {
            mRecorder.start();
            CLog.d(CLog.RECORDER, "MediaRecorder successfully started");
            return true;
        } catch (final IllegalStateException e) {
            e.printStackTrace();
            CLog.e(CLog.RECORDER, "MediaRecorder start failed - " + e.toString());
            return false;
        } catch (final RuntimeException e2) {
            e2.printStackTrace();
            CLog.e(CLog.RECORDER, "MediaRecorder start failed - " + e2.toString());
            mRecorderInterface.onRecordingFailed("Unable to record video with given settings");
            return false;
        }
    }

    protected boolean isRecording() {
        return mRecording;
    }

    public MediaRecorder getMediaRecorder() {
        return mRecorder;
    }

    public void lockVideoFile() {
        mVideoFile.setLocked(true);
        mVideoFileList.set(mFileIndex, mVideoFile);
    }

    private void setNextVideoFile() {
        int sz = mVideoFileList.size();
        for(int n=1; n<=sz; n++) {
            int fileIndex = (mFileIndex + n) % sz;
            if(!mVideoFileList.get(fileIndex).getLocked()) {
                mFileIndex = fileIndex;
                mVideoFile = mVideoFileList.get(fileIndex);
                break;
            }
        }
    }

    private void releaseRecorderResources() {
//        MediaRecorder recorder = mRecorder;
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void releasePreviewResources(){
        if (mVideoCapturePreview != null) {
            mVideoCapturePreview.releasePreviewResources();
        }
    }

    public void releaseAllResources() {
        if (mVideoCapturePreview != null) {
            mVideoCapturePreview.releasePreviewResources();
        }
        if (mCameraWrapper != null) {
            mCameraWrapper.releaseCamera();
            mCameraWrapper = null;
        }
        releaseRecorderResources();
        CLog.d(CLog.RECORDER, "Released all resources");
    }

    @Override
    public void onCapturePreviewFailed() {
        mRecorderInterface.onRecordingFailed("Unable to show camera preview");
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                // NOP
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                CLog.d(CLog.RECORDER, "MediaRecorder max duration reached");
                stopRecording("Capture looping - Max duration reached");

                // loop recording
                setNextVideoFile();
                startRecording();
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                CLog.d(CLog.RECORDER, "MediaRecorder max filesize reached");
                stopRecording("Capture looping - Max file size reached");

                // loop recording
                setNextVideoFile();
                startRecording();
                break;
            default:
                break;
        }
    }

}


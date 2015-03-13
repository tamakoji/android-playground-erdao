# Introduction #
Camera app with android face detection, captures cropped face.

# Detail #
It uses android.media.FaceDetector for face detection, though it works slow actually.

Thus, default preview size is set to 160x120( but actually it will be set to 176x144 ).

If this parameter is set higher(e.g. 320x240, btw if set to 320x240 preview size will be 320x240... strange)
the analysis thread cost increases much and not suitable for tracking face.

Also the optical characters of the camera for dream/magic is poor, lighting condition may affect the detection accuracy.

Sample trial to use jni interface is also included, currently only does is convert 8bit grayscale image to 32bit array.

# Source Codes #
http://code.google.com/p/android-playground-erdao/source/browse/#svn/trunk/SnapFace

# ScreenShot #
<img src='http://android-playground-erdao.googlecode.com/svn/wiki/img/snapface_sc01.png' height='240'>
<img src='http://android-playground-erdao.googlecode.com/svn/wiki/img/snapface_sc02.png' height='240'>
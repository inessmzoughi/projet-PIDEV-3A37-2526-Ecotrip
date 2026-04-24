package tn.esprit.services.face;

import org.opencv.core.Mat;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.FaceDescriptorUtil;

public class FaceVerificationService {

    private final HuggingFaceFaceService hfService    = new HuggingFaceFaceService();
    private final WebcamService          webcamService = new WebcamService();

    /**
     * Verify the current user's face against their stored descriptor.
     *
     * Mirrors PHP FaceController::verifyGate():
     * 1. Get stored descriptor from session user
     * 2. Get live embedding from webcam frame via HuggingFace
     * 3. Compare with cosine similarity
     *
     * @return true = same face (access granted), false = no match or no descriptor
     */
    public boolean verifyCurrentUser(Mat liveFrame) throws Exception {
        String storedJson = SessionManager.getInstance()
                .getCurrentUser().getFaceDescriptor();

        // Mirrors PHP: if (!$user->getFaceDescriptor()) return forbidden
        if (!FaceDescriptorUtil.isEnrolled(storedJson))
            return false;

        // Convert frame to bytes and call API
        byte[] imageBytes    = webcamService.matToJpegBytes(liveFrame);
        float[] liveEmbedding = hfService.getEmbeddingFromImage(imageBytes);
        if (liveEmbedding == null) return false;

        // Compare
        // Mirrors PHP: $this->faceService->compare($stored, $embedding)
        float[] storedEmbedding = FaceDescriptorUtil.decode(storedJson);
        return HuggingFaceFaceService.isSameFace(storedEmbedding, liveEmbedding);
    }
}
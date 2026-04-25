package tn.esprit.services.Auth_User.FaceRecognition;

import org.opencv.core.Mat;
import tn.esprit.models.Auth_User.User;
import tn.esprit.repository.Auth_User.UserRepository;
import tn.esprit.session.SessionManager;
import tn.esprit.utils.FaceDescriptorUtil;

public class FaceEnrollmentService {

    private final HuggingFaceFaceService hfService     = new HuggingFaceFaceService();
    private final WebcamService          webcamService  = new WebcamService();
    private final UserRepository         userRepository = new UserRepository();

    /**
     * Full enrollment flow:
     * 1. Mat frame → JPEG bytes
     * 2. Send to HuggingFace → float[] embedding
     * 3. JSON-encode → save to user.face_descriptor in DB
     *
     * Mirrors PHP FaceController::enroll()
     *
     * @return true if enrolled successfully
     */
    public boolean enrollFace(Mat frame) throws Exception {
        // Step 1: convert frame to JPEG bytes
        byte[] imageBytes = webcamService.matToJpegBytes(frame);
        if (imageBytes == null || imageBytes.length == 0)
            throw new RuntimeException("Cannot convert webcam frame to JPEG");

        // Step 2: call HuggingFace API
        float[] embedding = hfService.getEmbeddingFromImage(imageBytes);
        if (embedding == null || embedding.length == 0)
            return false;

        // Step 3: encode and save
        // Mirrors PHP: $user->setFaceDescriptor(array_map('floatval', $embedding));
        //              $this->em->flush();
        String descriptorJson = FaceDescriptorUtil.encode(embedding);

        User user = SessionManager.getInstance().getCurrentUser();
        user.setFaceDescriptor(descriptorJson);
        userRepository.saveFaceDescriptor(user.getId(), descriptorJson);
        SessionManager.getInstance().setCurrentUser(user);

        return true;
    }
}
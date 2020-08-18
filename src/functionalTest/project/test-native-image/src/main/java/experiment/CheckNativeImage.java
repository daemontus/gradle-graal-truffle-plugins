package experiment;

import org.graalvm.nativeimage.ImageInfo;

class CheckNativeImage {

    public static void main(String[] args) {
        if (ImageInfo.inImageCode()) {
            System.out.println("Execution success.");
        } else {
            System.out.println("Execution fail.");
            System.exit(100);
        }
    }

}
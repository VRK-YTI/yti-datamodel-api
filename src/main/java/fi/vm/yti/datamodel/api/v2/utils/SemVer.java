package fi.vm.yti.datamodel.api.v2.utils;

public class SemVer {

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;

    //Long regex string, but it's the one defined in semver.org (see near bottom of page)
    public static final String VALID_REGEX = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
    public SemVer(String version) {
        if(version.contains("+")){
            version = version.substring(0, version.indexOf("+"));
        }
        var versionSplit = version.split("\\.", 3);
        major = Integer.parseInt(versionSplit[0]);
        minor = Integer.parseInt(versionSplit[1]);
        if(versionSplit[2].contains("-")) {
            var split = versionSplit[2].split("-", 2);
            patch = Integer.parseInt(split[0]);
            preRelease = split[1];
        }else{
            patch = Integer.parseInt(versionSplit[2]);
            preRelease = null;
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getPreRelease() {
        return preRelease;
    }

    /**
     * Compares two semver strings
     * @param a First semver string
     * @param b Second semver string
     * @return negative if a is smaller, 0 if equal, positive if a is larger
     */
    public static int compareSemVers(String a, String b){
        if(a != null && b == null) {
            return 1;
        }
        if(a == null && b != null) {
            return -1;
        }
        if(a == null && b == null) {
            return 0;
        }

        var semVerA = new SemVer(a);
        var semVerB = new SemVer(b);

        if(semVerA.getMajor() != semVerB.getMajor()) {
            return Integer.compare(semVerA.getMajor(), semVerB.getMajor());
        }
        if(semVerA.getMinor() != (semVerB.getMinor())){
            return Integer.compare(semVerA.getMinor(), semVerB.getMinor());
        }
        if(semVerA.getPatch() != (semVerB.getPatch())){
            return Integer.compare(semVerA.getPatch(), semVerB.getPatch());
        }

        if(semVerA.getPreRelease() != null && semVerB.getPreRelease() == null) {
            return -1;
        }
        if(semVerA.getPreRelease() == null && semVerB.getPreRelease() != null) {
            return 1;
        }
        if(semVerA.getPreRelease() != null && semVerB.getPreRelease() != null){
            return comparePreRelease(semVerA.getPreRelease(), semVerB.getPreRelease());
        }

        return 0;
    }

    private static int comparePreRelease(String a, String b) {
        var aSplit = a.split("\\.");
        var bSplit = b.split("\\.");

        for(int i=0; i< aSplit.length &&i< bSplit.length; i++){
            if(aSplit[i].equals(bSplit[i])) {
                return aSplit[i].compareTo(bSplit[i]);
            }
        }
        return bSplit.length - aSplit.length;
    }
}

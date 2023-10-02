package fi.vm.yti.datamodel.api.v2.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemVerTest {

    @Test
    void testSemverSortList() {
        //This is a randomly generated list of valid semvers
        var preSorted = List.of(
                "0.4.1-rc2",
                "0.8.7",
                "1.0.0-alpha",
                "1.0.1-alpha.1",
                "1.2.3-rc1",
                "1.5.0",
                "1.9.0",
                "2.0.0-alpha.2",
                "2.0.0",
                "2.0.4",
                "3.3.5-rc5",
                "4.2.1-rc4",
                "4.7.8-rc3",
                "6.1.3-alpha.6",
                "7.2.6",
                "7.4.9",
                "8.0.6-alpha.9",
                "8.9.1-alpha.4",
                "9.3.7-alpha.3",
                "9.7.0-rc8"
        );

        var list = new ArrayList<>(List.of(
                "1.0.0-alpha",
                "2.0.0",
                "1.2.3-rc1",
                "1.0.1-alpha.1",
                "2.0.0-alpha.2",
                "1.5.0",
                "0.4.1-rc2",
                "9.3.7-alpha.3",
                "7.2.6",
                "4.7.8-rc3",
                "8.9.1-alpha.4",
                "2.0.4",
                "4.2.1-rc4",
                "1.9.0",
                "3.3.5-rc5",
                "0.8.7",
                "6.1.3-alpha.6",
                "7.4.9",
                "9.7.0-rc8",
                "8.0.6-alpha.9"
        ));

        list.sort(SemVer::compareSemVers);
        assertEquals(list, preSorted);
    }

    @Test
    void testSemverSortPreReleaseList(){
        //This list is from https://semver.org/#spec-item-11 See section 11.4
        var preReleaseListSorted = List.of(
                "1.0.0-alpha",
                "1.0.0-alpha.1",
                "1.0.0-alpha.beta",
                "1.0.0-beta",
                "1.0.0-beta.2",
                "1.0.0-beta.11",
                "1.0.0-rc.1",
                "1.0.0");
        var sortablePreReleaseList = new ArrayList<>(preReleaseListSorted);
        //order should not change when sorting
        sortablePreReleaseList.sort(SemVer::compareSemVers);
        assertEquals(preReleaseListSorted, sortablePreReleaseList);
    }

    @Test
    void testSemverCompare() {
        //Second is Larger https://semver.org/#spec-item-2
        assertTrue(SemVer.compareSemVers("1.0.0","1.0.1") < 0);
        //Second is smaller
        assertTrue(SemVer.compareSemVers("2.0.1","1.2.3") > 0);
        //Same
        assertEquals(0, SemVer.compareSemVers("2.0.0", "2.0.0"));
        //Same because build information doesn't matter https://semver.org/#spec-item-10
        assertEquals(0, SemVer.compareSemVers("2.0.0", "2.0.0+buildinfo"));
        //Same as build info doesn't matter even in pre-release
        assertEquals(0, SemVer.compareSemVers("2.0.0-beta", "2.0.0-beta+buildinfo"));
        //Actual release is larger than pre-release https://semver.org/#spec-item-9
        assertTrue(SemVer.compareSemVers("2.0.0","2.0.0-alpha") > 0);
    }
}

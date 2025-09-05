package media.library.flys.model;


public class PageEvArgsModel {

    //user當前頁面 * local * minty
    private String currentUserPage;

    //af拉取到的user來源 * or * non-or

    public String getMintyAFUser() {
        return mintyAFUser;
    }

    public void setMintyAFUser(String mintyAFUser) {
        this.mintyAFUser = mintyAFUser;
    }

    public String getMintyMediaSource() {
        return mintyMediaSource;
    }

    public void setMintyMediaSource(String mintyMediaSource) {
        this.mintyMediaSource = mintyMediaSource;
    }

    public String getMintyCampaign() {
        return mintyCampaign;
    }

    public void setMintyCampaign(String mintyCampaign) {
        this.mintyCampaign = mintyCampaign;
    }

    private String mintyAFUser;

    //af拉取到的media來源



    public String getCurrentUserPage() {
        return currentUserPage;
    }

    public void setCurrentUserPage(String currentUserPage) {
        this.currentUserPage = currentUserPage;
    }



    private String mintyMediaSource;

    //af拉取到的活動

    private String mintyCampaign;

    public String getGoogleRefer() {
        return googleRefer;
    }

    public void setGoogleRefer(String googleRefer) {
        this.googleRefer = googleRefer;
    }

    //google refer
    private String googleRefer;




}

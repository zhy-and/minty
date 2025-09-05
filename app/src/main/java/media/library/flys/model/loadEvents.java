package media.library.flys.model;


public class loadEvents {


    public loadEvents(boolean bannerType) {
        this.bannerType = bannerType;
    }

    public Boolean getBannerType() {
        return bannerType;
    }

    public void setBannerType(boolean bannerType) {
        this.bannerType = bannerType;
    }

    private boolean bannerType;
}

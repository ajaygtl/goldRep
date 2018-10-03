package com.equant.csi.interfaces.cis;

/**
 * Created by IntelliJ IDEA.
 * Date: Sep 22, 2004
 * Time: 5:51:59 PM
 */
public class RemoteSiteCodeInfo {
    public long versionServiceElementId;
    public long attributeId;
    public String USID;

    public RemoteSiteCodeInfo(long versionServiceElementId, long attributeId, String USID) {
        this.versionServiceElementId = versionServiceElementId;
        this.attributeId = attributeId;
        this.USID = USID;
    }
}

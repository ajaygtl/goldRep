/**
 * $Equant$
 * 
 * (c) Equant: GOLD 4.3 [Global Order Lifecycle Delivery] 
 * Date: Jul 21, 2004
 */
package com.equant.csi.common;

import org.apache.commons.lang.StringUtils;

/**
 * $Comment
 * 
 * @author Vasyl Rublyov
 * @version $Revision$
 */
public class ServiceIdHolder {
    public final String customerId;
    public final String siteId;
    public final String servicerId;

    public ServiceIdHolder(String customerId, String siteId, String servicerId) {
        this.customerId = customerId;
        this.siteId = siteId;
        this.servicerId = servicerId;
    }
    
    public boolean equals(Object obj) {
        if(obj instanceof ServiceIdHolder) {
            boolean ret = true;
            ServiceIdHolder id = (ServiceIdHolder)obj;
            if(id==null) return false;
            if(    StringUtils.equals(id.customerId, this.customerId)
                && StringUtils.equals(id.siteId, this.siteId)
                && StringUtils.equals(id.servicerId, this.servicerId)
            ) return true;
            return false;
        }
        return super.equals(obj);
    }
    
    public String toString() {
        return "ServiceIdHolder[customerId=\"" + customerId + "\", siteId=\"" + siteId + "\", servicerId=\"" + servicerId + "\"]";
    }
}

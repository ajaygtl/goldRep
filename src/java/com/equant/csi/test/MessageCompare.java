/**
 * Created by IntelliJ IDEA.
 * User: admin
 * Date: Dec 4, 2002
 * Time: 5:35:57 PM
 * To change this template use Options | File Templates.
 */
package com.equant.csi.test;

import java.util.List;

import org.apache.log4j.Category;

import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.NoteType;
import com.equant.csi.jaxb.NotesType;
import com.equant.csi.jaxb.OnceOffChargeType;
import com.equant.csi.jaxb.RecurringChargeType;
import com.equant.csi.jaxb.ServiceElementAttributeType;
import com.equant.csi.jaxb.ServiceElementStatusType;
import com.equant.csi.jaxb.ServiceElementType;
import com.equant.csi.jaxb.TypeType;
import com.equant.csi.jaxb.UsageChargeType;
import com.equant.csi.jaxb.UserType;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.common.TransDouble;

public class MessageCompare
{
    private static final Category logger = LoggerFactory.getInstance (MessageCompare.class.getName ());

    public static boolean compare (Message serviceDetails, Message serviceDetailsLegacy)
    {
        List tmp = null;
        List tmpL = null;
        compare ("COMPARE VERSIONS");

        boolean result = true;
        NotesType notes = serviceDetails.getNotes ();
        NotesType notesL = serviceDetailsLegacy.getNotes ();
        if (notes != null) {
            tmp = notes.getNote ();
            for (int i = 0; i < tmp.size (); i++) {
                NoteType note = (NoteType) tmp.get (i);
                compare ("NOTE " + i + " :\t " + note.getValue ());
            }
        }
        if (notesL != null) {
            tmpL = notesL.getNote ();
            for (int i = 0; i < tmpL.size (); i++) {
                NoteType noteL = (NoteType) tmpL.get (i);
                compare ("NOTE " + i + " :\t " + noteL.getValue ());
            }
        }

        tmp = serviceDetails.getVersion ();
        tmpL = serviceDetailsLegacy.getVersion ();
        if (tmp.size () == tmpL.size ()) {
            for (int i = 0; i < tmp.size (); i++) {
                VersionType ver = (VersionType) tmp.get (i);
                VersionType verL = (VersionType) tmpL.get (i);
                result &= compare ("VERSION " + i + " Orderid:\t\t ", ver.getOrderid (), verL.getOrderid ());
                result &= compare ("VERSION " + i + " Ordertype:\t\t ", ver.getOrdertype (), verL.getOrdertype ());
                result &= compare ("VERSION " + i + " Orderstatus:\t\t ", ver.getOrderstatus (), verL.getOrderstatus ());
                result &= compare ("VERSION " + i + " ContractId:\t\t ", ver.getContractId (), verL.getContractId ());
                result &= compare ("VERSION " + i + " SubbillingAccountId:\t ", "" + ver.getSubbillingAccountId (), "" + verL.getSubbillingAccountId ());
                result &= compare ("VERSION " + i + " SiteId:\t\t ", ver.getSiteId (), verL.getSiteId ());
				result &= compare ("VERSION " + i + " CoreSiteID:\t\t ", ver.getCoreSiteID (), verL.getCoreSiteID ());
				result &= compare ("VERSION " + i + " AddressID:\t\t ", ver.getAddressID (), verL.getAddressID ());
                result &= compare ("VERSION " + i + " customerId:\t\t ", ver.getCustomerId (), verL.getCustomerId ());
                result &= compare ("VERSION " + i + " EndUserId:\t\t ", ver.getEndUserId (), verL.getEndUserId ());
                result &= compare ("VERSION " + i + " ServiceId:\t\t ", ver.getServiceId (), verL.getServiceId ());
                result &= compare ("VERSION " + i + " Currency:\t\t ", ver.getCurrency (), verL.getCurrency ());
                result &= compare ("VERSION " + i + " ExchangeRate:\t\t ", TransDouble.printToString(ver.getExchangeRate()), TransDouble.printToString(verL.getExchangeRate ()));
                result &= compare ("VERSION " + i + " OrderSentDate:\t\t ", ver.getOrderSentDate (), verL.getOrderSentDate ());

                com.equant.csi.jaxb.SystemType sys = ver.getSystem ();
                com.equant.csi.jaxb.SystemType sysL = verL.getSystem ();
                if (sys != null && sysL != null) {
                    compare ("SYSTEM Id:\t\t\t ", sys.getId (), sysL.getId ());
                    UserType user = sys.getUser ();
                    UserType userL = sysL.getUser ();
                    result &= compare ("USER Name:\t\t ", user.getName (), userL.getName ());
                } else if (sys != null || sysL != null) {
                    compare ("System are not equals. (System=" + notes + ", SystemLegacy=" + notesL + ")");
                    result = false;
                }

                List seList = ver.getServiceElement ();
                List seListL = verL.getServiceElement ();
                for (int j = 0; j < seList.size (); j++) {
                    ServiceElementType se = (ServiceElementType) seList.get (j);
                    ServiceElementType seL = null;
                    for (int jj = 0; jj < seListL.size (); jj++) {
                        seL = (ServiceElementType) seListL.get (jj);
                        if (se.getId ().equals (seL.getId ())) {
                            break;
                        }
                        seL = null;
                    }
                    if (seL == null) {
                        compare ("ServiceElement (Id: " + se.getId () + ") not found in ServiceElementLegacy");
                        result = false;
                    } else {
                        result &= compare(j, se, seL);
                    }
                }
            }
        } else {
            compare ("Versions are not equals! (Versions.size=" + tmp.size () + ", VersionsLegacy.size=" + tmpL.size () + ")");
            result = false;
        }

        if (result) compare ("VERSIONS ARE THE SAME");
        else compare ("VERSIONS DIFFER");
        return result;
    }

    // Compare two ServiceElements
    private static boolean compare (int j, ServiceElementType se, ServiceElementType seL) {
        boolean result = true;
        result &= compare ("ELEMENT " + j + " Id:\t\t\t ", se.getId (), seL.getId ());
        result &= compare ("ELEMENT " + j + " UpdatedID:\t\t ", se.getUpdatedID (), seL.getUpdatedID ());
        result &= compare ("ELEMENT " + j + " ServiceElementClass:\t ", se.getServiceElementClass (), seL.getServiceElementClass ());
        result &= compare ("ELEMENT " + j + " Name:\t\t\t ", se.getName (), seL.getName ());
        result &= compare ("ELEMENT " + j + " Description:\t\t ", se.getDescription (), seL.getDescription ());
        result &= compare ("ELEMENT " + j + " GrandfatherDate:\t ", se.getGrandfatherDate (), seL.getGrandfatherDate ());
        result &= compare ("ELEMENT " + j + " CreationDate:\t\t ", se.getCreationDate (), seL.getCreationDate ());
        result &= compare ("ELEMENT " + j + " ProductId:\t\t ", se.getProductId (), seL.getProductId ());

        TypeType type = se.getType ();
        TypeType typeL = seL.getType ();
        compare ("TYPE Category:\t\t ", type.getCategory (), typeL.getCategory ());

        List sesList = se.getServiceElementStatus ();
        List sesListL = seL.getServiceElementStatus ();
        if (sesList.size () == sesListL.size ()) {
            for (int k = 0; k < sesList.size (); k++) {
                ServiceElementStatusType ses = (ServiceElementStatusType) sesList.get (k);
                ServiceElementStatusType sesL = (ServiceElementStatusType) sesListL.get (k);

                result &= compare ("STATUS " + k + " status:\t\t ", ses.getStatus (), sesL.getStatus ());
                result &= compare ("STATUS " + k + " date:\t\t\t ", ses.getDate (), sesL.getDate ());
            }
        } else {
            compare ("ServiceElementStatuses are not equals! (ServiceElementStatuses.size=" + sesList.size () + ", ServiceElementStatusesLegacy.size=" + sesListL.size () + ")");
            result = false;
        }

        List seaList = se.getServiceElementAttribute ();
        List seaListL = seL.getServiceElementAttribute ();
        for (int k = 0; k < seaList.size (); k++) {
            ServiceElementAttributeType sea = (ServiceElementAttributeType) seaList.get (k);
            ServiceElementAttributeType seaL = null;
            for (int kk = 0; kk < seaListL.size (); kk++) {
                seaL = (ServiceElementAttributeType) seaListL.get (kk);
                if (sea.getName ().equals (seaL.getName ())) {
                    break;
                }
                seaL = null;
            }
            if (seaL == null) {
                compare ("ServiceElementAttribute (Name: " + sea.getName () + ") not found in ServiceElementLegacy");
                result = false;
            } else {
                result &= compare (k, sea, seaL);
            }
        }

        OnceOffChargeType ooc = se.getOnceOffCharge ();
        OnceOffChargeType oocL = seL.getOnceOffCharge ();
        if (ooc != null && oocL != null) {
            result &= compare ("ELEMENT " + j + " ONCEOFF ChargeCategoryId:\t ", ooc.getChargeCategoryId (), oocL.getChargeCategoryId ());
            result &= compare ("ELEMENT " + j + " ONCEOFF ChangeTypeCode:\t ", ooc.getChangeTypeCode (), oocL.getChangeTypeCode ());
            result &= compare ("ELEMENT " + j + " ONCEOFF Amount:\t\t ", TransDouble.printToString(ooc.getAmount ()), TransDouble.printToString(oocL.getAmount ()));
            result &= compare ("ELEMENT " + j + " ONCEOFF DiscountCode:\t ", ooc.getDiscountCode (), oocL.getDiscountCode ());
        } else if (ooc != null || oocL != null) {
            compare ("OnceOffCharge are not equals. (OnceOffCharge=" + ooc + ", OnceOffChargeLegacy=" + oocL + ")");
            result = false;
        }

        RecurringChargeType rc = se.getRecurringCharge ();
        RecurringChargeType rcL = seL.getRecurringCharge ();
        if (rc != null && rcL != null) {
            result &= compare ("ELEMENT " + j + " RECURRING ChargeCategoryId:\t ", rc.getChargeCategoryId (), rcL.getChargeCategoryId ());
            result &= compare ("ELEMENT " + j + " RECURRING ChangeTypeCode:\t ", rc.getChangeTypeCode (), rcL.getChangeTypeCode ());
            result &= compare ("ELEMENT " + j + " RECURRING Amount:\t\t ", TransDouble.printToString(rc.getAmount ()), TransDouble.printToString(rcL.getAmount ()));
            result &= compare ("ELEMENT " + j + " RECURRING DiscountCode:\t ", rc.getDiscountCode (), rcL.getDiscountCode ());
        } else if (rc != null || rcL != null) {
            compare ("RecurringCharge are not equals. (RecurringCharge=" + rc + ", RecurringChargeLegacy=" + rcL + ")");
            result = false;
        }

        UsageChargeType uc = se.getUsageCharge ();
        UsageChargeType ucL = seL.getUsageCharge ();
        if (uc != null && ucL != null) {
            result &= compare ("ELEMENT " + j + " USAGE ChargeCategoryId:\t ", uc.getChargeCategoryId (), ucL.getChargeCategoryId ());
            result &= compare ("ELEMENT " + j + " USAGE ChangeTypeCode:\t ", uc.getChangeTypeCode (), ucL.getChangeTypeCode ());
        } else if (uc != null || ucL != null) {
            compare ("UsageCharge are not equals. (UsageCharge=" + uc + ", UsageChargeLegacy=" + ucL + ")");
            result = false;
        }
        return result;
    }

    private static boolean compare (int k, ServiceElementAttributeType sea, ServiceElementAttributeType seaL) {
        boolean result = true;
        result &= compare ("ATTRIBUTE " + k + " Name:\t\t\t ", sea.getName (), seaL.getName ());
        result &= compare ("ATTRIBUTE " + k + " Value:\t\t ", sea.getValue (), seaL.getValue ());
        result &= compare ("ATTRIBUTE " + k + " ChangeTypeCode:\t ", sea.getChangeTypeCode (), seaL.getChangeTypeCode ());
        result &= compare ("ATTRIBUTE " + k + " Status:\t\t ", sea.getStatus (), seaL.getStatus ());

        OnceOffChargeType ooc = sea.getOnceOffCharge ();
        OnceOffChargeType oocL = seaL.getOnceOffCharge ();
        if (ooc != null && oocL != null) {
            result &= compare ("ATTRIBUTE " + k + " ONCEOFF ChargeCategoryId:\t ", ooc.getChargeCategoryId (), oocL.getChargeCategoryId ());
            result &= compare ("ATTRIBUTE " + k + " ONCEOFF ChangeTypeCode:\t ", ooc.getChangeTypeCode (), oocL.getChangeTypeCode ());
            result &= compare ("ATTRIBUTE " + k + " ONCEOFF Amount:\t\t ", TransDouble.printToString(ooc.getAmount ()), TransDouble.printToString(oocL.getAmount ()));
            result &= compare ("ATTRIBUTE " + k + " ONCEOFF DiscountCode:\t ", ooc.getDiscountCode (), oocL.getDiscountCode ());
        } else if (ooc != null || oocL != null) {
            compare ("OnceOffCharge are not equals. (OnceOffCharge=" + ooc + ", OnceOffChargeLegacy=" + oocL + ")");
            result = false;
        }

        RecurringChargeType rc = sea.getRecurringCharge ();
        RecurringChargeType rcL = seaL.getRecurringCharge ();
        if (rc != null && rcL != null) {
            result &= compare ("ATTRIBUTE " + k + " RECURRING ChargeCategoryId:\t ", rc.getChargeCategoryId (), rcL.getChargeCategoryId ());
            result &= compare ("ATTRIBUTE " + k + " RECURRING ChangeTypeCode:\t ", rc.getChangeTypeCode (), rcL.getChangeTypeCode ());
            result &= compare ("ATTRIBUTE " + k + " RECURRING Amount:\t\t ", TransDouble.printToString(rc.getAmount ()), TransDouble.printToString(rcL.getAmount ()));
            result &= compare ("ATTRIBUTE " + k + " RECURRING DiscountCode:\t ", rc.getDiscountCode (), rcL.getDiscountCode ());
        } else if (rc != null || rcL != null) {
            compare ("RecurringCharge are not equals. (RecurringCharge=" + rc + ", RecurringChargeLegacy=" + rcL + ")");
            result = false;
        }

        UsageChargeType uc = sea.getUsageCharge ();
        UsageChargeType ucL = seaL.getUsageCharge ();
        if (uc != null && ucL != null) {
            result &= compare ("ATTRIBUTE " + k + " USAGE ChargeCategoryId:\t ", uc.getChargeCategoryId (), ucL.getChargeCategoryId ());
            result &= compare ("ATTRIBUTE " + k + " USAGE ChangeTypeCode:\t ", uc.getChangeTypeCode (), ucL.getChangeTypeCode ());
        } else if (uc != null || ucL != null) {
            compare ("UsageCharge are not equals. (UsageCharge=" + uc + ", UsageChargeLegacy=" + ucL + ")");
            result = false;
        }
        return result;
    }

    private static void compare (String line)
    {
        // This method aligns output of comparison
        logger.debug (line);
    }

    private static boolean compare (String name, Object value1, Object value2)
    {
        // Uncomment to not display equal values
        if (value1 != null && !value1.equals (value2) || value1 == null && value2 != null) {
            logger.debug (name + value1 + " | " + value2);
            return false;
        } else {
//            logger.debug(name + value1 + " | " + value2);
            return true;
        }
    }
}
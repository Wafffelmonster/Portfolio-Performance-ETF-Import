package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import enums.SecurityType;
import models.Security;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

public class SecurityService {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    public static Security[] processSecurities(NodeList oAllSecurities, List<SecurityType> userSecurityTypes) {
        Security[] allSecurity = new Security[oAllSecurities.getLength()];
        //System.out.printf("oAllSecurities.getLength(): %s\n", oAllSecurities.getLength());
        for (int i = 0; i < oAllSecurities.getLength(); i++) {
            Node oSecurity = oAllSecurities.item(i);
            Element element = (Element) oSecurity;
            Node oIsinNode = element.getElementsByTagName("isin").item(0);
            Node oNameNode = element.getElementsByTagName("name").item(0);
            Node isRetired = element.getElementsByTagName("isRetired").item(0);

            if (oIsinNode != null && !oIsinNode.getTextContent().equals("") &&
                    isRetired != null && isRetired.getTextContent().equals("false")) {
                //System.out.printf("oSecurity: %s\n", element.getElementsByTagName("isin").item(0).getTextContent());
                System.out.printf("Fetching data for \"" + oIsinNode.getTextContent() + "\"...");
                allSecurity[i] = createETF(oIsinNode.getTextContent(), userSecurityTypes);
                if (oNameNode != null) {
                    allSecurity[i].setName(oNameNode.getTextContent());
                }
                System.out.printf(" done!\n");
            }
        }

        return allSecurity;
    }

    private static boolean isETF(String strResponseFromFirstCall, List<SecurityType> allowedSecurities) {
        if (strResponseFromFirstCall.startsWith("/etf/anlageschwerpunkt")
                && allowedSecurities.contains(SecurityType.ETF)) {
            return true;
        }
        return false;
    }

    private static boolean isFond(String strResponseFromFirstCall, List<SecurityType> allowedSecurities) {
        if (strResponseFromFirstCall.startsWith("/fonds/anlageschwerpunkt")
                && allowedSecurities.contains(SecurityType.FOND)) {
            return true;
        }
        return false;
    }

    //, SecurityType[] securityType
    private static Security createETF(String strIsin, List<SecurityType> userSecurityTypes) {
        Security oSecurity = new Security(strIsin);
        try {
            String strResponseFromFirstCall = readStringFromURL("https://www.onvista.de/etf/anlageschwerpunkt/" + strIsin);
            System.out.println(strResponseFromFirstCall);
            oSecurity.setName(strResponseFromFirstCall.split("/")[2]);
            //System.out.printf("strResponseFromFirstCall: %s\n", strResponseFromFirstCall);

            // Check if ETF or fond
            if (isETF(strResponseFromFirstCall, userSecurityTypes)
                    || isFond(strResponseFromFirstCall, userSecurityTypes)) {

                String strResponseOfRequest = readStringFromURL("https://www.onvista.de" + strResponseFromFirstCall);

                String[] splitResponse = strResponseOfRequest.split("<script id=\"__NEXT_DATA__\" type=\"application/json\">");
                String responseAsJSONString = splitResponse[1].split("</script>")[0];

                JsonObject rootObj = JsonParser.parseString(responseAsJSONString).getAsJsonObject();
                JsonObject breakdownsNode = rootObj.getAsJsonObject("props").getAsJsonObject("pageProps").getAsJsonObject("data").getAsJsonObject("breakdowns");

                // parsing holdings
                if (breakdownsNode != null) {
                    JsonObject tempObjectHoldings = breakdownsNode.getAsJsonObject("fundsHoldingList");
                    JsonArray oArrayHoldingList = tempObjectHoldings != null ? tempObjectHoldings.getAsJsonArray("list") : new JsonArray();
                    Map<String, Security.PercentageUsedTuple> oListForHoldings = new HashMap<String, Security.PercentageUsedTuple>();

                    for (int i = 0; i < oArrayHoldingList.size(); i++) {
                        JsonObject oHolding = ((JsonObject) oArrayHoldingList.get(i));
                        String strHoldingName = oHolding.getAsJsonObject("instrument").get("name").getAsString();
                        Double nHoldingPercent = oHolding.get("investmentPct").getAsDouble();
                        //System.out.printf("Holding: %s; Percentage: %s%%\n", strHoldingName, df.format(nHoldingPercent));
                        oListForHoldings.put(strHoldingName, oSecurity.new PercentageUsedTuple(nHoldingPercent, false));
                    }
                    oSecurity.setHoldings(oListForHoldings);

                    // parsing branches
                    oSecurity.setBranches(getListForNode(breakdownsNode.getAsJsonObject("branchBreakdown"), false));

                    // parsing currency
                    oSecurity.setCurrencies(getListForNode(breakdownsNode.getAsJsonObject("currencyBreakdown"), false));

                    // parsing instrument
                    oSecurity.setInstruments(getListForNode(breakdownsNode.getAsJsonObject("instrumentBreakdown"), false));

                    // parsing country
                    oSecurity.setCountries(getListForNode(breakdownsNode.getAsJsonObject("countryBreakdown"), false));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return oSecurity;
    }

    private static Map<String, Security.PercentageUsedTuple> getListForNode(JsonObject oNode, boolean fPrint) {
        Map<String, Security.PercentageUsedTuple> oResultList = new HashMap<String, Security.PercentageUsedTuple>();
        if (oNode != null) {
            JsonArray oArrayList = oNode.getAsJsonArray("list");
            Security e = new Security("isin");
            for (int i = 0; i < oArrayList.size(); i++) {
                JsonObject oNodeInsideArray = ((JsonObject) oArrayList.get(i));
                String strName = oNodeInsideArray.get("nameBreakdown").getAsString();
                Double nPercent = oNodeInsideArray.get("investmentPct").getAsDouble();
                if (!strName.equals("Barmittel") || oNode.get("nameFundsBreakdown").getAsString().equals("Instrument")) {
                    oResultList.put(strName, e.new PercentageUsedTuple(nPercent, false));
                    if (fPrint) {
                        System.out.printf("name: %s; Percentage: %s%%\n", strName, DECIMAL_FORMAT.format(nPercent));
                    }
                }
            }
        }
        return oResultList;
    }

    private static String readStringFromURL(String requestURL) {
        Scanner scanner = null;
        String result = "";
        try {
            scanner = new Scanner(new URL(requestURL).openStream(), StandardCharsets.UTF_8.toString());
            scanner.useDelimiter("\\A");
            result = scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
        return result;
    }

}

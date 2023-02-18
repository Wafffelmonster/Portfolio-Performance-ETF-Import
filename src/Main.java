import enums.SecurityType;
import org.w3c.dom.Document;

import org.w3c.dom.NodeList;

import models.Security;

import java.util.*;

import static constants.PathConstants.BASE_PATH;
import static constants.PathConstants.FILE_NAME;
import static services.SecurityService.processSecurities;
import static xml.XmlFileReader.getAllSecurities;
import static xml.XmlFileReader.getDocument;
import static xml.XmlFileWriter.updateXml;

public class Main {

    public static void main(String[] args) {
        System.out.printf("----- Start -----\n");
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        Document doc = getDocument(BASE_PATH + FILE_NAME);
        NodeList allSecurities = getAllSecurities(doc);

        List<SecurityType> userSecurityTypes = inputSecurities();

        Security[] allSecuritiesProcessed = processSecurities(allSecurities, userSecurityTypes);
        updateXml(doc, allSecuritiesProcessed);

        //System.out.printf("name: %s; isin: %s\n", oEtf.getName(), oEtf.getIsin())
        System.out.printf("----- END -----\n");
    }

    private static List<SecurityType> inputSecurities() {
        System.out.printf("\n\nChoose security types:\n- ETF\n- Fond\n- all\n");
        Scanner userInput = new Scanner(System.in);
        String input = userInput.nextLine().toLowerCase();

        List<SecurityType> inputSecurities = new ArrayList<>();

        if (input.equals("etf")) {
            inputSecurities.add(SecurityType.ETF);
        }
        if (input.equals("fond")) {
            inputSecurities.add(SecurityType.FOND);
        }
        if (input.equals("all")) {
            inputSecurities.add(SecurityType.ETF);
            inputSecurities.add(SecurityType.FOND);
        }

        return inputSecurities;
    }
}
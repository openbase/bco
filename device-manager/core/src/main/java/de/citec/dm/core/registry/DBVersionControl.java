package de.citec.dm.core.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.citec.jul.exception.CouldNotPerformException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DBVersionControl {

    protected final Logger logger = LoggerFactory.getLogger(DBVersionControl.class);

    public void validateDBVersion(final File db) {
        int currenVersion = detectCurrendDBVersion(db);
        int latestVersion = detectLatestDBVersion();

        if (currenVersion == latestVersion) {
            return;
        }

        upgradeDB(currenVersion, latestVersion, final File db);

    }

    public void upgradeDB(final int currentVersion, final int targetVersion) {

        List<DBVersionConverter> converterPipeline = loadDBConverterPipeline(currenVersion, latestVersion);

        File entry = new File();

        for (DBVersionConverter converter : converterPipeline) {
            converter.upgrade(null)
        }

        String deviceConfigString = FileUtils.readFileToString(file, "UTF-8");
        JsonObject deviceConfig = new JsonParser().parse(deviceConfigString).getAsJsonObject();

        try {
            deviceConfigString = deviceConfig.toString();

//            System.out.println("### pre: " + jsonString);
//            System.out.println("######################");
            // format
            JsonElement el = parser.parse(deviceConfigString);
            jsonString = gson.toJson(el);
//            System.out.println("### after: " + jsonString);
//            System.out.println("######################");
            //write
            FileUtils.writeStringToFile(file, jsonString, "UTF-8");
            return file;
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize " + transformer + " into " + file + "!", ex);
        }
    }

    public int detectLatestDBVersion() {
        return 0;
    }

    /**
     * Method detects the current db version and returns the version number.
     *
     * @return
     */
    public int detectCurrendDBVersion(final File db) {
        return 0;
    }

    public List<DBVersionConverter> loadDBConverterPipeline(final int currentVersion, final int targetVersion) {
        List<DBVersionConverter> converterList = new ArrayList<>();

        return converterList;
    }
}

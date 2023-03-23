/**
 *
 *  $Id: TemplateDocumentFiller.java, v 0.0.2 2007/10/02 17:20    sdv Exp $
 *  $Id: TemplateLibreFiller.java,    v 0.0.1 2023-03-07 15:05:22 sdv Exp $
 *
 *  Copyright (C) 2006-2007 Dmitry Starzhynski
 *  Copyright (C) 2023 Dmitri Starzyński
 *
 *  File :               TemplateDocumentFiller.java
 *  File :               TemplateLibreFiller.java
 *  Description :        Replace template strings in OpenOffice
 *  Author's email :     dvstar@users.sourceforge.net
 *                       dmvstar.devel@gmail.com
 *  Author's Website :   http://swirl.sourceforge.net
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * Created on 31 серпня 2007, 10:32
 *
 */
package org.stardust.libreoffice.libretempla;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.table.XTableColumns;
import com.sun.star.table.XTableRows;
import com.sun.star.text.XText;
import com.sun.star.text.XTextTable;
import com.sun.star.text.XTextTablesSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XReplaceDescriptor;
import com.sun.star.util.XReplaceable;
import com.sun.star.util.XSearchable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Replace in OpenOffice document (writer or calc) template string to values
 * from data file.
 *
 * @author Starjinsky Dmitry
 * @version 0.0.2
 */
public class TemplateLibreFiller implements TemplateConstants {

    /**
     * OpenOffice UNO remote context
     */
    private XComponentContext mxRemoteContext = null;
    /**
     * OpenOffice UNO remote service manager
     */
    private XMultiComponentFactory mxRemoteServiceManager = null;
    
    /**
     * Main class and main method
     *
     * @param args JSON data file name
     */
    public static void main(String[] args) {
        System.out.println("Hello TemplateLibreFiller ! " + args.length);
        TemplateLibreFiller textDocumentsFiller = new TemplateLibreFiller();
        try {
            if (args.length > 0) {
                TemplateDataFile templateDataFile = new TemplateDataFile(args[0], OUT_FILE_TYPE_SAME);
                templateDataFile.buildData();
                System.out.println(templateDataFile);
                textDocumentsFiller.templateDataFile = templateDataFile;                
                //textDocumentsFiller.templateFieldsDataMap = templateDataFile.createTemplateDataMap();
            } else {
                TemplateExampleData templateExampleData = new TemplateExampleData();
                templateExampleData.buildData();
                textDocumentsFiller.templateDataFile = templateExampleData;   
                System.out.println("Hello TemplateLibreFiller ! " + templateExampleData);
                //textDocumentsFiller.templateFieldsDataMap = templateExampleData.createTemplateDataMap();
            }            
            textDocumentsFiller.processTemplate(textDocumentsFiller.templateDataFile.getTemplateDocumentFileName());
            
        } catch (TemplateException | BootstrapException | com.sun.star.uno.Exception | InterruptedException | IOException ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("end ...");
            System.exit(0);
        }
    }

    public static void usage() {
        System.out.println("Not enough arguments.");
        System.out.println("  Usage:");
        System.out.println("  TemplateLibreFiller <DataFileName>");
    }
    private ITemplateDataFile templateDataFile;

    public TemplateLibreFiller() {
    }

    /**
     * Run process template fill
     *
     * @param templateDocumentFileName
     * @throws TemplateException
     * @throws IOException
     * @throws BootstrapException
     * @throws com.sun.star.uno.Exception
     * @throws InterruptedException
     */
    protected void processTemplate(String templateDocumentFileName)
            throws TemplateException, IOException, BootstrapException, com.sun.star.uno.Exception, InterruptedException {
        XComponent xTemplateComponent;
        String templateDocumentFileURL = templateDataFile.getTemplateDocumentFileURL();
        System.out.println("sTemplateFileUrl = " + templateDocumentFileURL);
        xTemplateComponent = prepareDocComponentFromTemplate(templateDocumentFileURL);

        XReplaceable xReplaceable;
        com.sun.star.util.XSearchable xSearchable;
        HashMap templateFieldsDataMap = templateDataFile.getTemplateDataMap();
        
        Iterator keyIterator = templateFieldsDataMap.keySet().iterator();
        //while(keyIterator.hasNext()){
        //    keyIterator.next(); 
        //}
        //Enumeration keys = templateFieldsDataMap.keys();
        xReplaceable = (XReplaceable) UnoRuntime.queryInterface(
                XReplaceable.class, xTemplateComponent);
        xSearchable = UnoRuntime.queryInterface(
                com.sun.star.util.XSearchable.class, xTemplateComponent);

        int curCount = 0;
        //while (keys.hasMoreElements()) {
        while (keyIterator.hasNext()) {
            //String key = (String) keys.nextElement();
            String key = (String) keyIterator.next();
            Object oval = templateFieldsDataMap.get(key);
            String val = (String) oval.toString();
            if (oval instanceof String) {
                //Thread.sleep(500);
                if (findWordTemplate(xSearchable, "${" + key + "}")) {
                    replaceWordTemplate(++curCount, xReplaceable, "${" + key + "}", val);
                }
            } else {
                if (oval instanceof ArrayList) {
                    replaceTableTemplate(xTemplateComponent, xReplaceable, key, (ArrayList) oval);
                }
            }
        }
    }

    /**
     * Replace single template in document
     *
     * @param curCount
     * @param xReplaceable
     * @param frStr Temlate string like ${user}
     * @param toStr Value string for template like star
     */
//    protected void replaceWordTemplate(int curCount, XComponent xTemplateComponent, String frStr, String toStr) {
    protected void replaceWordTemplate(int curCount, com.sun.star.util.XReplaceable xReplaceable, String frStr, String toStr) {
        XReplaceDescriptor xReplaceDescr;
        System.out.println("[" + curCount + "] Replace " + frStr + " -> " + toStr);
        //xReplaceable = (com.sun.star.util.XReplaceable) UnoRuntime.queryInterface(
        //        com.sun.star.util.XReplaceable.class, xTemplateComponent);
        // You need a descriptor to set properies for Replace
        if (xReplaceable != null) {
            xReplaceDescr = (XReplaceDescriptor) xReplaceable.createReplaceDescriptor();
            xReplaceDescr.setSearchString(frStr);
            xReplaceDescr.setReplaceString(toStr);
            // Replace all words
            xReplaceable.replaceAll(xReplaceDescr);
        }
    }

    /**
     * Replacr table template with array data
     * @param xTemplateComponent
     * @param xReplaceable
     * @param tableName name of table
     * @param tableItems array of rows
     * @throws WrappedTargetException
     * @throws IndexOutOfBoundsException
     */
    protected void replaceTableTemplate(XComponent xTemplateComponent,
            com.sun.star.util.XReplaceable xReplaceable,
            String tableName,
            ArrayList<HashMap> tableItems)
            throws WrappedTargetException, IndexOutOfBoundsException {

        XTextTablesSupplier xTablesSupplier = (XTextTablesSupplier) UnoRuntime.queryInterface(XTextTablesSupplier.class, xTemplateComponent);
        XNameAccess xNamedTables = xTablesSupplier.getTextTables();
        //System.out.println("xNamedTables " + xNamedTables);
        System.out.println("replaceTableTemplate  " + tableName + "[" + tableItems.size() + "]");
        try {
            Object oTable = xNamedTables.getByName(tableName);
            if (oTable != null) {
                //System.out.println("oTable " + oTable);
                XTextTable xTable = (XTextTable) UnoRuntime.queryInterface(XTextTable.class, oTable);
                XTableRows xRows = xTable.getRows();
                XTableColumns xCols = xTable.getColumns();
                System.out.println("xTable " + xTable + " " + xRows.getCount() + " " + tableItems.size());
                //  XRow xRow = xRows.getByIndex(1);
                xRows.insertByIndex(2, tableItems.size());
                XCellRange xCellRange = (XCellRange) UnoRuntime.queryInterface(XCellRange.class, oTable);
                int row = 2, count = 1;
                System.out.println("tableItems " + tableItems.size());
                for (int i = 0; i < tableItems.size(); i++) { // data
                    //System.out.println("items " + i);
                    // data item    
                    //Object items = tableItems.get(i);   
                    //System.out.println("items " + items.getClass());
                    //System.out.println("items " + items);
                    HashMap<String, String> items = tableItems.get(i);
                    Object keys[] = items.keySet().toArray();
                    for (int j = 0; j < xCols.getCount(); j++) { // cols           
                        XCell xCellTemp = xCellRange.getCellByPosition(j, 1);
                        XText xTextTemp = (XText) UnoRuntime.queryInterface(XText.class, xCellTemp);
                        XCell xCell = xCellRange.getCellByPosition(j, i + row);
                        XText xText = (XText) UnoRuntime.queryInterface(XText.class, xCell);

                        String cellDst = xTextTemp.getString();
                        if (xTextTemp.getString().equalsIgnoreCase("#{i}")) {
                            cellDst = "" + count;
                        } else {
                            // for (int k = 0; k < keys.length; k++) { // keys
                            for (Object key : keys) {
                                // keys
                                String testKey = "#{" + key + "}";
                                System.out.println("    [" + i + "] " + testKey + " -> " + items.get(key));
                                if (xTextTemp.getString().equalsIgnoreCase(testKey)) {
                                    cellDst = items.get(key.toString());
                                }
                                if (xTextTemp.getString().contains(testKey)) {
                                    cellDst = cellDst.replace(testKey, items.get(key.toString()));
                                }
                            }
//                    java.util.Set keys = templatePatternDataMap.keySet();
//                    java.util.Iterator iteratorKeys = keys.iterator();
//                    while(keyIterator.hasNext()){
//                    keyIterator.next();  
                            //xText.setString(tableName + "[" + i + "][" + j + "]");
                        }
                        xText.setString(cellDst);
                    }
                    count++;
                }
                xRows.removeByIndex(1, 1);
            }
        } catch (NoSuchElementException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load a document as template
     *
     * @param loadUrl Url for template document
     * @return Loaded instance of office document for UNO
     * @throws com.sun.star.comp.helper.BootstrapException
     * @throws com.sun.star.uno.Exception
     */
    protected XComponent prepareDocComponentFromTemplate(String loadUrl) throws BootstrapException, com.sun.star.uno.Exception {
        // get the remote service manager
        mxRemoteServiceManager = this.prepareRemoteServiceManager();
        // retrieve the Desktop object, we need its XComponentLoader
        Object desktop = mxRemoteServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", mxRemoteContext);
        XComponentLoader xComponentLoader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, desktop);

        // define load properties according to com.sun.star.document.MediaDescriptor
        // the boolean property AsTemplate tells the office to create a new document
        // from the given file
        PropertyValue[] loadProps = new PropertyValue[1];
        loadProps[0] = new PropertyValue();
        loadProps[0].Name = "AsTemplate";
        loadProps[0].Value = new Boolean(true);
        // load
        return xComponentLoader.loadComponentFromURL(loadUrl, "_blank", 0, loadProps);
    }

    /**
     * Getting remote service manager for OpenOffice
     *
     * @throws java.lang.Exception Exception
     * @return Instance of remote service manager
     */
    private XMultiComponentFactory prepareRemoteServiceManager() throws BootstrapException {
        //String oooExeFolder = "/usr/lib/libreoffice/program";
        if (mxRemoteContext == null && mxRemoteServiceManager == null) {
            // get the remote office context. If necessary a new office
            // process is started
            mxRemoteContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
            //mxRemoteContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            System.out.println("Connected to a running office ...");
            mxRemoteServiceManager = mxRemoteContext.getServiceManager();
        }
        return mxRemoteServiceManager;
    }

    private boolean findWordTemplate(XSearchable xSearchable, String sSearchString) throws UnknownPropertyException, PropertyVetoException, IllegalArgumentException, WrappedTargetException {
        boolean ret = false;
        com.sun.star.util.XSearchDescriptor xSearchDescriptor;
        com.sun.star.uno.XInterface xSearchInterface;
        xSearchDescriptor = xSearchable.createSearchDescriptor();
        xSearchDescriptor.setSearchString(sSearchString);
        com.sun.star.beans.XPropertySet xPropertySet;
        xPropertySet = UnoRuntime.queryInterface(
                com.sun.star.beans.XPropertySet.class, xSearchDescriptor);
        xPropertySet.setPropertyValue("SearchRegularExpression",
                Boolean.FALSE);
        xSearchInterface = (com.sun.star.uno.XInterface) xSearchable.findFirst(xSearchDescriptor);
        //System.out.println("xSearchInterface " + xSearchInterface);
        if (xSearchInterface != null) {
            ret = true;
        }
        return ret;
    }

    /*
    private HashMap createTemplateDataMap() throws TemplateException, IOException {
        HashMap ret = new HashMap();
        if (templateJSON != null) {
            JSONArray data = templateJSON.getJSONArray(TEMPLATE_DATA_KEY);
            for (int i = 0; i < data.length(); i++) {
                //Iterator<?> keys = data.getJSONObject(i).keys();
                JSONObject itemj = data.getJSONObject(i);
                //System.out.println("    Item: " + itemj);
                String key = itemj.getString(DATA_KEY);
                Object val = itemj.get(DATA_VAL);
                //System.out.println("        Key: " + key);
                //System.out.println("        Class: " + val.getClass());
                //System.out.println("        Value: " + val);
                if (val instanceof String) {
                    if (((String) val).contains("NOW()")) {
                        val = getNowDateTime();
                    }
                    ret.put(key, val);
                }
                if (val instanceof JSONArray) {
                    JSONArray valj = (JSONArray) val;
                    ArrayList list = new ArrayList();
                    for (int ii = 0; ii < valj.length(); ii++) {
                        JSONArray itemsj = valj.getJSONArray(ii);
                        //System.out.println("        LValue: " + itemsj);
                        HashMap itemsa = new HashMap();
                        for (int j = 0; j < itemsj.length(); j++) {
                            JSONObject itemaj = itemsj.getJSONObject(j);
                            //System.out.println("            LValue: " + itemaj);
                            String akey = itemaj.getString(DATA_KEY);
                            String aval = itemaj.getString(DATA_VAL);
                            itemsa.put(akey, aval);
                        }
                        list.add(itemsa);
                    }
                    ret.put(key, list);
                }
            }
        } else {
            throw new TemplateException("No JSON dara");
        }
        //throw new UnsupportedOperationException("Not supported yet.");
        return ret;
    }

    private String getNowDateTime() {
        String ret;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        ret = dtf.format(now);
        return ret;
    }
    */
}
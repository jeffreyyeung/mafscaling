/*
* Open-Source tuning tools
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.vgi.mafscaling.openloop;

import com.vgi.mafscaling.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

public class OpenLoopJY extends AMafScaling {
    private static final Logger logger = Logger.getLogger(OpenLoopJY.class);
    private static final String SaveDataFileHeader = "[open_loop run data]";
    private static final String RunTableName = "Run ";
    private static final String Y2AxisName = "AFR Error (%)";
    private static final String rpmAxisName = "RPM";
    private static final String runDataName = "AFR Error";
    private static final int RunCount = 10;
    private static final int RunRowsCount = 200;

    private int clValue = Config.getClOlStatusValue();
    private double minMafV = Config.getMafVMinimumValue();
    private double afrErrPrct = Config.getWidebandAfrErrorPercentValue();
    private double minWotEnrichment = Config.getWOTEnrichmentValue();
    private int wotPoint = Config.getWOTStationaryPointValue();
    private int afrRowOffset = Config.getWBO2RowOffset();
    private int skipRowsOnTransition = Config.getOLCLTransitionSkipRows();

    private int logThtlAngleColIdx = -1;
    private int logMafvColIdx = -1;
    private int logAfrColIdx = -1;
    private int logRpmColIdx = -1;
    private int logLoadColIdx = -1;
    private int logMapColIdx = -1;
    private int logCommandedAfrColIdx = -1;
    private int logClOlStatusColIdx = -1;

    private JCheckBox checkBoxMafRpmData = null;
    private final ArrayList<Double> afrArray = new ArrayList<>();
    private final ArrayList<JTable> runTables = new ArrayList<>();
    private final ArrayList<JButton> removeButtons = new ArrayList<>();
    private GridBagLayout gbl_dataRunPanel = null;
    private GridBagConstraints gbc_run = null;
    JPanel dataRunPanel = null;

    public OpenLoopJY(int tabPlacement, PrimaryOpenLoopFuelingTable table, MafCompare comparer) {
        super(tabPlacement, table, comparer);
        logger.setLevel(Level.ALL);
        runData = new XYSeries(runDataName);
        initialize();
    }

    //////////////////////////////////////////////////////////////////////////////////////
    // DATA TAB
    //////////////////////////////////////////////////////////////////////////////////////
    
    protected void createRunPanel(JPanel dataPanel) {
        JScrollPane dataScrollPane = new JScrollPane();
        GridBagConstraints gbc_dataScrollPane = new GridBagConstraints();
        gbc_dataScrollPane.weightx = 1.0;
        gbc_dataScrollPane.weighty = 1.0;
        gbc_dataScrollPane.fill = GridBagConstraints.BOTH;
        gbc_dataScrollPane.gridx = 0;
        gbc_dataScrollPane.gridy = 3;
        dataPanel.add(dataScrollPane, gbc_dataScrollPane);
        
        dataRunPanel = new JPanel();
        dataScrollPane.setViewportView(dataRunPanel);
        gbl_dataRunPanel = new GridBagLayout();
        gbl_dataRunPanel.columnWidths = new int[RunCount];
        gbl_dataRunPanel.rowHeights = new int[] {0};
        gbl_dataRunPanel.columnWeights = new double[RunCount];
        gbl_dataRunPanel.columnWeights[RunCount -1] = 1.0;
        gbl_dataRunPanel.rowWeights = new double[]{0.0};
        dataRunPanel.setLayout(gbl_dataRunPanel);

        createRunTables();
    }
    
    private void createRunTables() {
        gbc_run = new GridBagConstraints();
        gbc_run.anchor = GridBagConstraints.PAGE_START;
        gbc_run.insets = new Insets(0, 2, 0, 2);
        gbc_run.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < RunCount; ++i)
            addRunTable();
    }
    
    private void addRunTable() {
        JTable table = new JTable();
        runTables.add(table);
        table.getTableHeader().setReorderingAllowed(false);
        table.setModel(new DefaultTableModel(RunRowsCount, 3));
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setBorder(new LineBorder(new Color(0, 0, 0)));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); 
        table.putClientProperty("terminateEditOnFocusLost", true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getColumnModel().getColumn(0).setHeaderValue("<html><center>Engine<br>Speed<br>(RPM)<br></center></html>");
        table.getColumnModel().getColumn(1).setHeaderValue("<html><center>MAF<br>Sensor<br>Voltage<br></center></html>");
        table.getColumnModel().getColumn(2).setHeaderValue("<html><center>AFR<br>Error<br>%<br></center></html>");
        Utils.initializeTable(table, ColumnWidth);
        excelAdapter.addTable(table, true, false);

        gbl_dataRunPanel.columnWidths = new int[runTables.size() + 1];
        gbl_dataRunPanel.columnWeights = new double[runTables.size() + 1];
        gbl_dataRunPanel.columnWeights[runTables.size()] = 1.0;

        gbc_run.gridx = runTables.size() - 1;
        gbc_run.gridy = 0;
        JButton jb = new JButton("remove");
        jb.setActionCommand("trem");
        jb.addActionListener(this);
        jb.setPreferredSize(new Dimension(150,30));
        removeButtons.add(jb);
        dataRunPanel.add(jb, gbc_run);
        gbc_run.gridy = 1;
        dataRunPanel.add(table.getTableHeader(), gbc_run);
        gbc_run.gridy = 2;
        dataRunPanel.add(table, gbc_run);
        repaint();
        revalidate();
    }
    
    private void removeRunTable(int index) {
        JButton jb = removeButtons.remove(index);
        JTable table = runTables.remove(index);
        
        excelAdapter.removeTable(table);
        dataRunPanel.remove(jb);
        dataRunPanel.remove(table.getTableHeader());
        dataRunPanel.remove(table);

        gbl_dataRunPanel.columnWidths = new int[runTables.size() + 1];
        gbl_dataRunPanel.columnWeights = new double[runTables.size() + 1];
        gbl_dataRunPanel.columnWeights[runTables.size()] = 1.0;
        
        for (int i = 0; i < runTables.size(); ++i) {
            jb = removeButtons.get(i);
            table = runTables.get(i);
            gbc_run.gridx = i;
            gbc_run.gridy = 0;
            dataRunPanel.add(jb, gbc_run);
            gbc_run.gridy = 1;
            dataRunPanel.add(table.getTableHeader(), gbc_run);
            gbc_run.gridy = 2;
            dataRunPanel.add(table, gbc_run);
        }
        repaint();
        revalidate();
    }

    //////////////////////////////////////////////////////////////////////////////////////
    // CREATE CHART TAB
    //////////////////////////////////////////////////////////////////////////////////////
    
    protected void createGraphTab() {
        JPanel cntlPanel = new JPanel();
        JPanel plotPanel = createGraphPlotPanel(cntlPanel);
        add(plotPanel, "<html><div style='text-align: center;'>C<br>h<br>a<br>r<br>t</div></html>");
        
        GridBagLayout gbl_cntlPanel = new GridBagLayout();
        gbl_cntlPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        gbl_cntlPanel.rowHeights = new int[]{0, 0};
        gbl_cntlPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_cntlPanel.rowWeights = new double[]{0};
        cntlPanel.setLayout(gbl_cntlPanel);
        
        GridBagConstraints gbc_check = new GridBagConstraints();
        gbc_check.insets = insets2;
        gbc_check.gridx = 0;
        gbc_check.gridy = 0;
        
        checkBoxMafRpmData = new JCheckBox("MafV/RPM");
        checkBoxMafRpmData.setActionCommand("mafrpm");
        checkBoxMafRpmData.addActionListener(this);
        cntlPanel.add(checkBoxMafRpmData, gbc_check);

        gbc_check.gridx++;
        checkBoxRunData = new JCheckBox("AFR Error");
        checkBoxRunData.setActionCommand("rdata");
        checkBoxRunData.addActionListener(this);
        cntlPanel.add(checkBoxRunData, gbc_check);

        gbc_check.gridx++;
        createGraphCommonControls(cntlPanel, gbc_check.gridx);

        createChart(plotPanel, Y2AxisName);        
        createMafSmoothingPanel(plotPanel);
    }

    //////////////////////////////////////////////////////////////////////////////////////
    // COMMON ACTIONS RELATED FUNCTIONS
    //////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void actionPerformed(ActionEvent e) {
        if (checkActionPerformed(e))
            return;
        if ("mafrpm".equals(e.getActionCommand())) {
            JCheckBox checkBox = (JCheckBox)e.getSource();
            if (checkBox.isSelected()) {
                clearNotRunDataCheckboxes();
                if (!plotMafVRpmData())
                    checkBox.setSelected(false);
            }
            else
                runData.clear();
            setRanges();
        }
        else if ("rdata".equals(e.getActionCommand())) {
            JCheckBox checkBox = (JCheckBox)e.getSource();
            if (checkBox.isSelected()) {
                if (checkBoxMafRpmData.isSelected()) {
                    checkBoxMafRpmData.setSelected(false);
                    runData.clear();
                }
                if (!plotRunData())
                    checkBox.setSelected(false);
            }
            else
                runData.clear();
            setRanges();
        }
        else if ("current".equals(e.getActionCommand())) {
            JCheckBox checkBox = (JCheckBox)e.getSource();
            if (checkBox.isSelected()) {
                if (checkBoxMafRpmData.isSelected()) {
                    checkBoxMafRpmData.setSelected(false);
                    runData.clear();
                }
                if (!plotCurrentMafData())
                    checkBox.setSelected(false);
            }
            else
                currMafData.clear();
            setRanges();
        }
        else if ("corrected".equals(e.getActionCommand())) {
            JCheckBox checkBox = (JCheckBox)e.getSource();
            if (checkBox.isSelected()) {
                if (checkBoxMafRpmData.isSelected()) {
                    checkBoxMafRpmData.setSelected(false);
                    runData.clear();
                }
                if (!setCorrectedMafData())
                    checkBox.setSelected(false);
            }
            else
                corrMafData.clear();
            setRanges();
        }
        else if ("smoothed".equals(e.getActionCommand())) {
            JCheckBox checkBox = (JCheckBox)e.getSource();
            if (checkBox.isSelected()) {
                if (checkBoxMafRpmData.isSelected()) {
                    checkBoxMafRpmData.setSelected(false);
                    runData.clear();
                }
                if (!setSmoothedMafData())
                    checkBox.setSelected(false);
            }
            else
                smoothMafData.clear();
            setRanges();
        }
        else if ("smoothing".equals(e.getActionCommand())) {
            JCheckBox checkBox = (JCheckBox)e.getSource();
            enableSmoothingView(checkBox.isSelected());
            setRanges();
        }
        else if ("trem".equals(e.getActionCommand())) {
            JButton sjb = (JButton) e.getSource();
            int idx = 0;
            for (JButton jb : removeButtons) {
                if (jb == sjb)
                    break;
                idx += 1;
            }
            removeRunTable(idx);
        }
    }
    
    protected void clearRunTables() {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            while (runTables.size() > RunCount)
                removeRunTable(runTables.size() - 1);
            for (JTable runTable : runTables) {
                while (RunRowsCount < runTable.getRowCount())
                    Utils.removeRow(RunRowsCount, runTable);
                Utils.clearTable(runTable);
            }
        }
        finally {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }
    
    protected void clearData() {
        super.clearData();
        afrArray.clear();
    }
    
    protected void clearChartCheckBoxes() {
        super.clearChartCheckBoxes();
        checkBoxMafRpmData.setSelected(false);
    }
    
    protected void calculateMafScaling() {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            clearData();
            clearChartData();
            clearChartCheckBoxes();

            TreeMap<Integer, ArrayList<Double>> result = new TreeMap<>();
            if (!getMafTableData(voltArray, gsArray))
                return;
            if (!sortRunData(result) || result.isEmpty())
                return;
            calculateCorrectedGS(result);
            setCorrectedMafData();
            
            smoothGsArray.addAll(gsCorrected);
            checkBoxCorrectedMaf.setSelected(true);
            
            setXYTable(mafSmoothingTable, voltArray, smoothGsArray);

            setRanges();
            setSelectedIndex(1);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            JOptionPane.showMessageDialog(null, "Error: " + e, "Error", JOptionPane.ERROR_MESSAGE);    
        }
        finally {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }
    
    private boolean sortRunData(TreeMap<Integer, ArrayList<Double>> result) {
        int closestVoltIdx;
        double rpm;
        double voltage;
        double error;
        ArrayList<Double> closestVolatageArray;
        for (int i = 0; i < runTables.size(); ++i) {
            JTable table = runTables.get(i);
            String tableName = RunTableName + (i + 1);
            String rpmValue;
            String mafvValue;
            String afrValue;
            for (int j = 0; j < table.getRowCount(); ++j) {
                rpmValue = table.getValueAt(j, 0).toString();
                mafvValue = table.getValueAt(j, 1).toString();
                afrValue = table.getValueAt(j, 2).toString();
                if (rpmValue.isEmpty() || mafvValue.isEmpty() || afrValue.isEmpty())
                    continue;
                if (!Utils.validateDouble(rpmValue, j, 0, tableName) ||
                    !Utils.validateDouble(mafvValue, j, 1, tableName) ||
                    !Utils.validateDouble(afrValue, j, 2, tableName))
                    return false;
                rpm = Double.parseDouble(rpmValue);
                voltage = Double.parseDouble(mafvValue);
                error = Double.parseDouble(afrValue);
                rpmArray.add(rpm);
                mafvArray.add(voltage);
                afrArray.add(error);
                closestVoltIdx = Utils.closestValueIndex(voltage, voltArray);
                closestVolatageArray = result.computeIfAbsent(closestVoltIdx, k -> new ArrayList<>());
                closestVolatageArray.add(error);
            }
        }
        return true;
    }
    
    private void calculateCorrectedGS(TreeMap<Integer, ArrayList<Double>> result) {
        ArrayList<Double> closestVolatageArray;
        double gs;
        double avgError;
        int lastErrIndex = 0;
        int i;
        gsCorrected.addAll(gsArray);
        for (i = 0; i < gsCorrected.size(); ++i) {
            gs = gsCorrected.get(i);
            avgError = 0;
            closestVolatageArray = result.get(i);
            if (closestVolatageArray != null) {
                for (Double aDouble : closestVolatageArray)
                    avgError += aDouble;
                avgError /= closestVolatageArray.size();
                lastErrIndex = i;
            }
            gsCorrected.set(i, gs * (1 + avgError/100.0));
        }
        avgError = 0;
        ArrayList<Double> sortedAfrArray = result.get(lastErrIndex);
        sortedAfrArray.sort(Collections.reverseOrder());
        for (i = 0; i < 10 && i < sortedAfrArray.size(); ++i)
            avgError += sortedAfrArray.get(i);
        if (i > 0)
            avgError /= i;
        for (i = lastErrIndex + 1; i < gsCorrected.size(); ++i) {
            gs = gsCorrected.get(i);
            gsCorrected.set(i, gs +(gs * 0.01 * avgError));
        }
    }

    private boolean plotMafVRpmData() {
        return setXYSeries(runData, rpmArray, mafvArray);
    }

    private boolean plotRunData() {
        return setXYSeries(runData, mafvArray, afrArray);
    }
    
    private void setRanges() {
        double paddingX;
        double paddingY;
        XYPlot plot = mafChartPanel.getChartPanel().getChart().getXYPlot();
        plot.getDomainAxis(0).setLabel(XAxisName);
        plot.getRangeAxis(1).setLabel(Y2AxisName);
        plot.getRangeAxis(0).setVisible(true);
        if (checkBoxMafRpmData.isSelected() && checkBoxMafRpmData.isEnabled()) {
            paddingX = runData.getMaxX() * 0.05;
            paddingY = runData.getMaxY() * 0.05;
            plot.getDomainAxis(0).setRange(runData.getMinX() - paddingX, runData.getMaxX() + paddingX);
            plot.getRangeAxis(1).setRange(runData.getMinY() - paddingY, runData.getMaxY() + paddingY);
            plot.getRangeAxis(0).setVisible(false);
            plot.getRangeAxis(1).setLabel(XAxisName);
            plot.getDomainAxis(0).setLabel(rpmAxisName);
        }
        else if (checkBoxRunData.isSelected() && checkBoxRunData.isEnabled() &&
                !checkBoxCurrentMaf.isSelected() && !checkBoxCorrectedMaf.isSelected() && !checkBoxSmoothedMaf.isSelected()) {
            paddingX = runData.getMaxX() * 0.05;
            paddingY = runData.getMaxY() * 0.05;
            plot.getDomainAxis(0).setRange(runData.getMinX() - paddingX, runData.getMaxX() + paddingX);
            plot.getRangeAxis(1).setRange(runData.getMinY() - paddingY, runData.getMaxY() + paddingY);
        }
        else if (checkBoxSmoothing.isSelected()) {
            double maxY = Collections.max(Arrays.asList(currMafData.getMaxY(), smoothMafData.getMaxY(), corrMafData.getMaxY()));
            double minY = Collections.min(Arrays.asList(currMafData.getMinY(), smoothMafData.getMinY(), corrMafData.getMinY()));
            paddingX = smoothMafData.getMaxX() * 0.05;
            paddingY = maxY * 0.05;
            plot.getDomainAxis(0).setRange(smoothMafData.getMinX() - paddingX, smoothMafData.getMaxX() + paddingX);
            plot.getRangeAxis(0).setRange(minY - paddingY, maxY + paddingY);
        }
        else if ((checkBoxCurrentMaf.isSelected() && checkBoxCurrentMaf.isEnabled()) ||
                 (checkBoxCorrectedMaf.isSelected() && checkBoxCorrectedMaf.isEnabled()) ||
                 (checkBoxSmoothedMaf.isSelected() && checkBoxSmoothedMaf.isEnabled())) {
            paddingX = voltArray.get(voltArray.size() - 1) * 0.05;
            paddingY = gsCorrected.get(gsCorrected.size() - 1) * 0.05;
            plot.getDomainAxis(0).setRange(voltArray.get(0) - paddingX, voltArray.get(voltArray.size() - 1) + paddingX);
            plot.getRangeAxis(0).setRange(gsCorrected.get(0) - paddingY, gsCorrected.get(gsCorrected.size() - 1) + paddingY);
            if (checkBoxRunData.isSelected()) {
                paddingY = runData.getMaxY() * 0.05;
                plot.getRangeAxis(1).setRange(runData.getMinY() - paddingY, runData.getMaxY() + paddingY);
            }
        }
        else {
            plot.getRangeAxis(0).setAutoRange(true);
            plot.getDomainAxis(0).setAutoRange(true);
        }
    }
    
    protected void onEnableSmoothingView(boolean flag) {
        checkBoxMafRpmData.setEnabled(!flag);
        if (!flag) {
            if (checkBoxMafRpmData.isSelected())
                plotMafVRpmData();
            if (checkBoxRunData.isSelected())
                plotRunData();
            if (checkBoxCurrentMaf.isSelected())
                plotCurrentMafData();
            if (checkBoxCorrectedMaf.isSelected())
                setCorrectedMafData();
            if (checkBoxSmoothedMaf.isSelected())
                setSmoothedMafData();
        }
    }
    
    protected void onSmoothReset() {
        if (!checkBoxMafRpmData.isEnabled() || !checkBoxMafRpmData.isSelected())
            setCorrectedMafData();
        if (checkBoxSmoothing.isSelected())
            plotSmoothingLineSlopes();
        else if (checkBoxSmoothedMaf.isSelected())
            setSmoothedMafData();
    }
   
    public void saveData() {
        if (JFileChooser.APPROVE_OPTION != fileChooser.showSaveDialog(this))
            return;
        File file = fileChooser.getSelectedFile();
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        int i, j;
        Writer out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Config.getEncoding()));
            // write string identifier
            out.write(SaveDataFileHeader + "\n");
            // write maf data
            for (i = 0; i < mafTable.getRowCount(); ++i) {
                for (j = 0; j < mafTable.getColumnCount(); ++j)
                    out.write(mafTable.getValueAt(i, j).toString() + ",");
                out.write("\n");
            }
            // write run data
            for (JTable table : runTables) {
                for (i = 0; i < table.getColumnCount(); ++i) {
                    for (j = 0; j < table.getRowCount(); ++j)
                        out.write(table.getValueAt(j, i)
                                       .toString() + ",");
                    out.write("\n");
                }
            }
        }
        catch (Exception e) {
            logger.error(e);
        }
        finally {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }
   
    public void loadData() {
        fileChooser.setMultiSelectionEnabled(false);
        if (JFileChooser.APPROVE_OPTION != fileChooser.showOpenDialog(this))
            return;
        File file = fileChooser.getSelectedFile();
        int i, j, k, l;
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsoluteFile()), Config.getEncoding()));
            String line = br.readLine();
            if (line == null || !line.equals(SaveDataFileHeader)) {
                JOptionPane.showMessageDialog(null, "Invalid saved data file!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            line = br.readLine();
            String[] elements;
            JTable table = null;
            i = k = l = 0;
            while (line != null) {
                elements = line.split(Utils.fileFieldSplitter, -1);
                switch (i) {
                    case 0, 1 -> {
                        Utils.ensureColumnCount(elements.length - 1, mafTable);
                        for (j = 0; j < elements.length - 1; ++j)
                            mafTable.setValueAt(elements[j], i, j);
                    }
                    default -> {
                        int offset = runTables.size() * 3 + mafTable.getRowCount();
                        if (i > 1 && i < offset) {
                            if (l == 0)
                                table = runTables.get(k++);
                            Utils.ensureRowCount(elements.length - 1, table);
                            for (j = 0; j < elements.length - 1; ++j)
                                table.setValueAt(elements[j], j, l);
                            l += 1;
                            if (l == 3)
                                l = 0;
                        }
                    }
                }
                i += 1;
                line = br.readLine();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
        finally {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            if (br != null) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }
    
    private boolean getColumnsFilters(String[] elements, boolean isPolfTableSet, boolean isPolfTableMap) {
        boolean ret = true;
        ArrayList<String> columns = new ArrayList<>(Arrays.asList(elements));
        String logThtlAngleColName = Config.getThrottleAngleColumnName();
        String logMafvColName = Config.getMafVoltageColumnName();
        String logAfrColName = Config.getWidebandAfrColumnName();
        String logRpmColName = Config.getRpmColumnName();
        String logLoadColName = Config.getLoadColumnName();
        String logMapColName = Config.getMapColumnName();
        String logCommandedAfrColName = Config.getCommandedAfrColumnName();
        String logClOlStatusColName = Config.getClOlStatusColumnName();
        logThtlAngleColIdx = columns.indexOf(logThtlAngleColName);
        logMafvColIdx = columns.indexOf(logMafvColName);
        logAfrColIdx = columns.indexOf(logAfrColName);
        logRpmColIdx = columns.indexOf(logRpmColName);
        logLoadColIdx = columns.indexOf(logLoadColName);
        logMapColIdx = columns.indexOf(logMapColName);
        logCommandedAfrColIdx = columns.indexOf(logCommandedAfrColName);
        logClOlStatusColIdx = columns.indexOf(logClOlStatusColName);
        if (logThtlAngleColIdx == -1)                                 { Config.setThrottleAngleColumnName(Config.NO_NAME); ret = false; }
        if (logMafvColIdx == -1)                                      { Config.setMafVoltageColumnName(Config.NO_NAME);    ret = false; }
        if (logAfrColIdx == -1)                                       { Config.setWidebandAfrColumnName(Config.NO_NAME);   ret = false; }
        if (logRpmColIdx == -1)                                       { Config.setRpmColumnName(Config.NO_NAME);           ret = false; }
        if (logClOlStatusColIdx == -1)                                { Config.setClOlStatusColumnName(Config.NO_NAME);   ret = false; }
        if (logCommandedAfrColIdx == -1) {
            if (isPolfTableSet) {
                if (!logCommandedAfrColName.equals(Config.NO_NAME)) {
                    JOptionPane.showMessageDialog(null, "'Commanded AFR' column is specified but was not found in the log file.\nResetting 'Commanded AFR' column to blank", "Invalid column", JOptionPane.WARNING_MESSAGE);
                    Config.setCommandedAfrColumnName(Config.NO_NAME);
                }
            }
            else {
                Config.setCommandedAfrColumnName(Config.NO_NAME);
                ret = false;
            }
        }
        if (logLoadColIdx == -1 && isPolfTableSet && !isPolfTableMap && logCommandedAfrColIdx == -1) {
            Config.setLoadColumnName(Config.NO_NAME);
            ret = false;
        }
        if (logMapColIdx == -1 && isPolfTableMap && logCommandedAfrColIdx == -1) {
            Config.setMapColumnName(Config.NO_NAME);
            ret = false;
        }

        wotPoint = Config.getWOTStationaryPointValue();
        clValue = Config.getClOlStatusValue();
        minMafV = Config.getMafVMinimumValue();
        afrErrPrct = Config.getWidebandAfrErrorPercentValue();
        minWotEnrichment = Config.getWOTEnrichmentValue();
        afrRowOffset = Config.getWBO2RowOffset();
        skipRowsOnTransition = Config.getOLCLTransitionSkipRows();
        return ret;
    }
    
    protected void loadLogFile() {
        boolean displayDialog = true;
        boolean isPolfSet = polfTable.isSet();
        boolean isPolfMap = polfTable.isMap();
        File[] files = fileChooser.getSelectedFiles();
        for (File file : files) {
            BufferedReader br = null;
            ArrayDeque<String[]> buffer = new ArrayDeque<>();
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsoluteFile()), Config.getEncoding()));
                String line;
                String [] elements = null;
                while ((line = br.readLine()) != null && (elements = line.split(Utils.fileFieldSplitter, -1)) != null && elements.length < 2)
                    continue;
                getColumnsFilters(elements, isPolfSet, isPolfMap);
                boolean resetColumns = false;
                if (logClOlStatusColIdx >= 0 || logThtlAngleColIdx >= 0 || logMafvColIdx >= 0 ||
                    logAfrColIdx >= 0 || logRpmColIdx >= 0 || logLoadColIdx >= 0 || logCommandedAfrColIdx >= 0 || logMapColIdx >= 0) {
                    if (displayDialog) {
                        int rc = JOptionPane.showOptionDialog(null, "Would you like to reset column names or filter values?", "Columns/Filters Reset", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionButtons, optionButtons[0]);
                        if (rc == 0)
                            resetColumns = true;
                        else if (rc == 2)
                            displayDialog = false;
                    }
                }
                
                if (resetColumns
                        || logClOlStatusColIdx < 0
                        || logThtlAngleColIdx < 0
                        || logMafvColIdx < 0
                        || logAfrColIdx < 0
                        || logRpmColIdx < 0
                        || (logLoadColIdx < 0 && !isPolfMap && isPolfSet && logCommandedAfrColIdx < 0)
                        || (logMapColIdx < 0 && isPolfMap && logCommandedAfrColIdx < 0)
                        || (logCommandedAfrColIdx < 0 && !isPolfSet)) {
                    ColumnsFiltersSelection selectionWindow = new OLJYColumnsFiltersSelection(isPolfSet, isPolfMap);
                    if (!selectionWindow.getUserSettings(elements) || !getColumnsFilters(elements, isPolfSet, isPolfMap))
                        return;
                }
                
                String[] flds;
                String[] afrflds;
                int clol;
                boolean isOL = true;
                boolean foundWot = false;
                double throttle;
                double afr;
                double rpm;
                double mafv;
                double cmdafr;
                double afrErr;
                double loadOrMap;
                int skipRowCount = 0;
                int logRow = 0;
                int runTableIndex = 0;
                int runTableRow = 0;
                for (; runTableIndex < runTables.size(); ++runTableIndex) {
                    if (runTables.get(runTableIndex).getValueAt(0, 0).toString().isEmpty())
                        break;
                }
                if (runTableIndex == runTables.size())
                    addRunTable();
                JTable runTable = runTables.get(runTableIndex);
                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                for (int k = 0; k <= afrRowOffset && line != null; ++k) {
                    line = br.readLine();
                    if (line != null)
                        buffer.addFirst(line.split(Utils.fileFieldSplitter, -1));
                }
                while (line != null && buffer.size() > afrRowOffset) {
                    afrflds = buffer.getFirst();
                    flds = buffer.removeLast();
                    line = br.readLine();
                    if (line != null)
                        buffer.addFirst(line.split(Utils.fileFieldSplitter, -1));

                    try {
                        throttle = Double.parseDouble(flds[logThtlAngleColIdx]);

                        if (flds[logClOlStatusColIdx].equals("on"))
                            clol = 0;
                        else if (flds[logClOlStatusColIdx].equals("off"))
                            clol = 1;
                        else
                            clol = (int)Utils.parseValue(flds[logClOlStatusColIdx]);

                        // If closed loop
                        if (clol == clValue) {
                            // Transition from OL to CL
                            if (isOL) {
                                isOL = false;
                                skipRowCount = 0;
                            }
                        }
                        // If open loop AND throttle position is above threshold
                        else if (throttle >= wotPoint) {
                            // Transition from CL to OL
                            if (!isOL) {
                                isOL = true;
                                skipRowCount = 0;
                                // If there was previous WOT run, increment to next run table
                                if (foundWot &&
                                    !runTable.getValueAt(0, 0).equals("") &&
                                    !runTable.getValueAt(1, 0).equals("") &&
                                    !runTable.getValueAt(2, 0).equals("")) {
                                    runTableIndex += 1;
                                    if (runTableIndex == runTables.size())
                                        addRunTable();
                                    runTable = runTables.get(runTableIndex);
                                }
                                if (logRow > 0)
                                    runTableRow = 0;
                            }

                            if (skipRowCount >= skipRowsOnTransition) {
                                mafv = Double.parseDouble(flds[logMafvColIdx]);
                                if (minMafV <= mafv) {
                                    foundWot = true;
                                    afr = Double.parseDouble(afrflds[logAfrColIdx]);
                                    // Convert AEM wideband voltage to AFR
                                    //afr = (Double.parseDouble(afrflds[logAfrColIdx]) * 2.375) + 7.3125;
                                    rpm = Double.parseDouble(flds[logRpmColIdx]);

                                    if (logCommandedAfrColIdx >= 0)
                                        cmdafr = Double.parseDouble(flds[logCommandedAfrColIdx]);
                                    else {
                                        loadOrMap = (isPolfMap ? Double.valueOf(flds[logMapColIdx]) : Double.valueOf(flds[logLoadColIdx]));
                                        cmdafr = Utils.calculateCommandedAfr(rpm, loadOrMap, minWotEnrichment, polfTable);
                                    }

                                    afrErr = (afr - cmdafr) / cmdafr * 100.0;
                                    if (Math.abs(afrErr) <= afrErrPrct) {
                                        Utils.ensureRowCount(runTableRow + 1, runTable);
                                        runTable.setValueAt(rpm, runTableRow, 0);
                                        runTable.setValueAt(mafv, runTableRow, 1);
                                        runTable.setValueAt(afrErr, runTableRow, 2);
                                        runTableRow += 1;
                                    }
                                }
                            }
                            skipRowCount += 1;
                        }
                    }
                    catch (NumberFormatException e) {
                        logger.error(e);
                        JOptionPane.showMessageDialog(null, "Error parsing number at " + file.getName() + " line " + (logRow + 1) + ": " + e, "Error processing file", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    logRow += 1;
                }

                if (!foundWot) {
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    JOptionPane.showMessageDialog(null, "Sorry, no WOT pulls were found in the log file", "No WOT data", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            catch (Exception e) {
                logger.error(e);
                JOptionPane.showMessageDialog(null, e, "Error opening file", JOptionPane.ERROR_MESSAGE);
            }
            finally {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                if (br != null) {
                    try {
                        br.close();
                    }
                    catch (IOException e) {
                        logger.error(e);
                    }
                }
            }
        }
    }
    
    protected String usage() {
        ResourceBundle bundle;
        bundle = ResourceBundle.getBundle("com.vgi.mafscaling.openloopjy");
        return bundle.getString("usage"); 
    }
}
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

package com.vgi.mafscaling;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class BgColorFormatRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -2283383526525346419L;
    private Color bgColor = getBackground();
    private Color[][] colors = null;

    /**
     * Method sets background color matrix for cells
     * @param colorMatrix
     */
    public void setColors(Color[][] colorMatrix) {
        colors = colorMatrix;
    }

    /**
     * Method returns background color matrix for cells
     * @return colorMatrix
     */
    public Color[][] getColors() {
        return colors;
    }
    
    /**
     * Method sets background color for a specific cell
     * @param color, background color
     * @param row, cell row index
     * @param column, cell column index
     */
    public void setColorAt(Color color, int row, int column) {
        if (colors != null && row < colors.length && column < colors[0].length)
            colors[row][column] = color;
    }
    
    /**
     * Method returns background color for a specific cell
     * @param row, cell row index
     * @param column, cell column index
     * @return Color
     */
    public Color getColorAt(int row, int column) {
        if (colors != null && row < colors.length && column < colors[0].length)
            return colors[row][column];
        return bgColor;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setForeground(Color.BLACK);
        if (colors != null) {
            if (row < colors.length && column < colors[0].length) {
                Color color = colors[row][column];
                if (calculateLuminance(color.getRed(), color.getGreen(), color.getBlue()) < 100) {
                    setForeground(Color.YELLOW);
                }
                setBackground(color);
            } else {
                setBackground(bgColor);
            }
        }
        else
            setBackground(bgColor);
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column );
    }

    private float calculateLuminance(int red, int green, int blue){
        return (float) (0.2126*red + 0.7151*green + 0.0721*blue);
    }
}

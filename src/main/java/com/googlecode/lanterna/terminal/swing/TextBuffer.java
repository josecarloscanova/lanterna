/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2014 Martin
 */
package com.googlecode.lanterna.terminal.swing;

import com.googlecode.lanterna.CJKUtils;
import com.googlecode.lanterna.terminal.TerminalPosition;
import com.googlecode.lanterna.terminal.TerminalSize;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author martin
 */
class TextBuffer {
    private final int backlog;
    private final LinkedList<List<TerminalCharacter>> lineBuffer;
    private final TerminalCharacter fillCharacter;

    TextBuffer(int backlog, TerminalCharacter fillCharacter) {
        this.backlog = backlog;
        this.lineBuffer = new LinkedList<List<TerminalCharacter>>();
        this.fillCharacter = fillCharacter;

        //Initialize the content to one line
        newLine();
    }

    void clear() {
        lineBuffer.clear();
        newLine();
    }

    void newLine() {
        ArrayList<TerminalCharacter> line = new ArrayList<TerminalCharacter>(200);
        line.add(fillCharacter);
        lineBuffer.addFirst(line);
    }


    Iterable<List<TerminalCharacter>> getVisibleLines(final int visibleRows, final int scrollOffset) {
        final int length = Math.min(visibleRows, lineBuffer.size());
        return new Iterable<List<TerminalCharacter>>() {
            @Override
            public Iterator<List<TerminalCharacter>> iterator() {
                return new Iterator<List<TerminalCharacter>>() {
                    private final ListIterator<List<TerminalCharacter>> listIter = lineBuffer.subList(0, length).listIterator(length);
                    @Override
                    public boolean hasNext() { return listIter.hasPrevious(); }
                    @Override
                    public List<TerminalCharacter> next() { return listIter.previous(); }
                    @Override
                    public void remove() { listIter.remove(); }   
                };
            }
        };
    }
    
    int getNumberOfLines() {
        return lineBuffer.size();
    }
    
    public void trimBacklog(int terminalHeight) {
        while(lineBuffer.size() - terminalHeight > backlog) {
            lineBuffer.removeLast();
        }
    }
    
    void ensurePosition(TerminalSize terminalSize, TerminalPosition position) {
        getLine(terminalSize, position);
    }
    
    
    void setCharacter(TerminalSize terminalSize, TerminalPosition currentPosition, TerminalCharacter terminalCharacter) {
        List<TerminalCharacter> line = getLine(terminalSize, currentPosition);
        line.set(currentPosition.getColumn(), terminalCharacter);
        
        //Pad CJK character with a trailing space
        if(CJKUtils.isCharCJK(terminalCharacter.getCharacter()) && currentPosition.getColumn() + 1 < line.size()) {
            ensurePosition(terminalSize, currentPosition.withRelativeColumn(1));
            line.set(currentPosition.getColumn() + 1, terminalCharacter.withCharacter(' '));
        }
        //If there's a CJK character immediately to our left, reset it
        if(currentPosition.getColumn() > 0 && CJKUtils.isCharCJK(line.get(currentPosition.getColumn() - 1).getCharacter())) {
            line.set(currentPosition.getColumn() - 1, line.get(currentPosition.getColumn() - 1).withCharacter(' '));
        }
    }

    private List<TerminalCharacter> getLine(TerminalSize terminalSize, TerminalPosition position) {
        while(position.getRow() >= lineBuffer.size()) {
            newLine();
        }
        List<TerminalCharacter> line = lineBuffer.get(Math.min(terminalSize.getRows(), lineBuffer.size()) - 1 - position.getRow());
        while(line.size() <= position.getColumn()) {
            line.add(fillCharacter);
        }
        return line;
    }
}
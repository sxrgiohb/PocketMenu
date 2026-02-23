package com.example.pocketmenu.data.model.auxiliar;

import java.util.Date;
import java.util.List;

public class DayMenuWrapper {
    private final int dayOfWeek;
    private final Date date;
    private final List<MenuAssignment> assignments;

    public DayMenuWrapper(int dayOfWeek, Date date, List<MenuAssignment> assignments) {
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.assignments = assignments;
    }

    public int getDayOfWeek() { return dayOfWeek; }
    public Date getDate() { return date; }
    public List<MenuAssignment> getAssignments() { return assignments; }
}
package com.example.pocketmenu.data.model.auxiliar;

import java.util.Date;
import java.util.List;

// This class wraps the data for a day's menu
public class DayMenuWrapper {
    private final int dayOfWeek;
    private final Date date;
    private final List<MenuAssignment> assignments;

    // Constructor
    public DayMenuWrapper(int dayOfWeek, Date date, List<MenuAssignment> assignments) {
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.assignments = assignments;
    }

    // Getters
    public int getDayOfWeek() { return dayOfWeek; }
    public Date getDate() { return date; }
    public List<MenuAssignment> getAssignments() { return assignments; }
}
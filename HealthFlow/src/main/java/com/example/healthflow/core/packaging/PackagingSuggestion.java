package com.example.healthflow.core.packaging;

public class PackagingSuggestion {
    public PackUnit unit;     // BLISTER/BOX/BOTTLE/TUBE/UNIT
    public int count;         // عدد العبوات المقترح
    public int unitsTotal;    // مجموع وحدات الأساس التي تغطيها
    public String label;      // نص عرض مختصر: "2 BLISTER = 20"
}
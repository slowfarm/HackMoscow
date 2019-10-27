package ru.eva.hackmoscow;

import ru.eva.hackmoscow.model.Geodata;

public interface OnGeodataRecieved {
    void onResponse(Geodata geodata);
    void onFailure(Throwable t);
}

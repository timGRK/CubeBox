package com.cube.logic;

import com.cube.event.CubeMsg;
import com.cube.exception.IllegalDataException;

public interface Process {

    public void excute(CubeMsg msg) throws IllegalDataException;
}

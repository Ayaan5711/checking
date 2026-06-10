package com.tcsion.ml.qsnauthorv2.inputtypes;

import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;

public interface AuthoringInterface {
    OutputBean questionAuthoring(InputBean inputBean) throws Exception;
}

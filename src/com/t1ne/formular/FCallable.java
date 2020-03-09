package com.t1ne.formular;

import java.util.List;

interface FCallable {
    int argsNum();
    Object call(Interpreter interpreter, List<Object> arguments);
}
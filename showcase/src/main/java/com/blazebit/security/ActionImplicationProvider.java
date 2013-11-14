package com.blazebit.security;

import java.util.List;
import java.util.Map;

public interface ActionImplicationProvider {

    public Map<Action, List<Action>> getActionImplications();
}

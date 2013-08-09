/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security;

import java.util.Collection;

/**
 *
 * @author Christian Beikov
 */
public interface Role<R extends Role<R, P, Q>, P extends Permission<?>, Q extends Permission<?>> {

    public R getParent();

    public Collection<P> getPermissions();

    public Collection<Q> getDataPermissions();
}

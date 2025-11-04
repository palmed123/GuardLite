/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package cnm.mixin.O.mapping;

public interface IMappingBuilder {
    static IMappingBuilder create(String... names) {
        return new NamedMappingFile(names == null || names.length == 0 ? new String[]{"left", "right"} : names);
    }

    IPackage addPackage(String... names);

    IClass addClass(String... names);

    INamedMappingFile build();

    interface IPackage {
        IPackage meta(String key, String value);

        IMappingBuilder build();
    }

    interface IClass {
        IField field(String... names);

        IMethod method(String descriptor, String... names);

        IClass meta(String key, String value);

        IMappingBuilder build();
    }

    interface IField {
        IField descriptor(String value);

        IField meta(String key, String value);

        IClass build();
    }

    interface IMethod {
        IParameter parameter(int index, String... names);

        IMethod meta(String key, String value);

        IClass build();
    }

    interface IParameter {
        IParameter meta(String key, String value);

        IMethod build();
    }
}

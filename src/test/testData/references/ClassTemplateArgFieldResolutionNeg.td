class Foo<int i>;

class Bar {
    int i = 0;
}

class Foobar<int i> : Bar, Foo<<caret>i>;
class Foo<int i>;

class Foobar<int i> : Foo<<caret>i> {
    int i = 0;
}
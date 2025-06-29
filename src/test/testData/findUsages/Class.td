
class <caret>A;

class B<A a> : A {
    list<A> l = [a];
}

defvar C = A<>;

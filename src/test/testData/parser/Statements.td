def {
    list<int> i = 1;
}

def a {
    list<int> i = 1;
}

def a # {
    list<int> i = 1;
}

include "test.td"

assert true, "string";

class EmptyBody;

class Inheriting : EmptyBody;

class EmptyBraces {
    bit b;
}

class Args<int i, int j = 0> : EmptyBraces {
    field int k = i;

    let b = 1;

    defvar hmm = 5;

    assert true, "string";

    dump b;
}

def;
def : EmptyBraces;
def Named : Args<0, j=1> {
    assert true, "is record body";
}

multiclass m {}
defm m;

class RISCVReg<bits<5> Enc> : Register<n> {
  let HWEncoding{4-0} = Enc;
}

class ParamNativeOpTrait<list<int> traits = []>;

class BaseTwoOperandRegReg<int i, bits<6> b>;
def : BaseTwoOperandRegReg<0b0, {0,0,0,0,1,?,}>;

multiclass Foo : m;

defvar l = !cond(
    true : false,
);

defvar t = l[0, ];

let HWEncoding{4-0} = {0, 0, 0, 0, 1} in {
    def : Register<5>;
}

let append i = [2] in {
    def {
        list<int> i = [1];
    }
}

let prepend i = [0] in {
    def {
        list<int> i = [1];
    }
}

let append = 1 in {
    def {
        int append = 0;
    }
}

let prepend = 1 in {
    def {
        int prepend = 0;
    }
}

class WithList {
    int append = 0;
    int prepend = 0;
    list<int> i = [1];
}
def : WithList {
    let append = 1;
    let append i = [2];
}
def : WithList {
    let prepend = 1;
    let prepend i = [0];
}

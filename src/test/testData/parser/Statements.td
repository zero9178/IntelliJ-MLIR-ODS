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

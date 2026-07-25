0

def

def Foo {
  int i = 0;
}

def Bar : Foo

def Foo {
  int i = 0;
}

class Foobar<>;

def Foo {
  int i = 0;
}

include

def Bar : Foo<

def Foo {
  int i = 0;
}

def Error {
  int i = ;
}

def Foo {
  int i = 0;
}

def Error {
  int i = Test<,;
}

def Foo {
  int i = 0;
}

class Foo<string s = "", Bar>;

class Foo<string s = "", > {
  int i = 0;
}


defvar value = [
def
      "GenerateOp", "PragmaOp",
class
      "IfOp", "ReduceOp", "ScanOp"
];

0

def lol;

def : Foo<0>, {
    int i = 0;
}

defvar d = (5 5, 3:, );

def IntTypesOp : TEST_Op<"int_types", >;

def {
    dag arguments = (ins
        StrAttr:$sym_name,
        Variadic<>
        OptionalAttr<StrAttr>:$sym_visibility,
        StrAttr:$sym_name
    );
}

def {
    let appnd i = [5];
}

defvar e1 = !switch(0, 1:, "d");
defvar after1 = 0;

defvar e2 = !switch(0, );
defvar after2 = 0;

defvar e3 = !switch(0, : "v", "d");
defvar after3 = 0;

def BrokenBangOperator {
  let name = !con
  let name = "list";
}
defvar afterBangOperator = 0;

defvar e4 = !cond;
defvar after4 = 0;

defvar e5 = !switch;
defvar after5 = 0;

defvar e6 = !foreach;
defvar after6 = 0;

defvar e7 = !cast;
defvar after7 = 0;

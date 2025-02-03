if true then <fold text='{...}'>{

}</fold> else <fold text='{...}'>{

}</fold>

foreach i = [0, 1] in <fold text='{...}'>{

}</fold>

let i = 0 in <fold text='{...}'>{

}</fold>

defvar i = Foo<fold text='<~>'><

></fold>;

def Foo : Bar<fold text='<~>'><

></fold> <fold text='{...}'>{

}</fold>

class Bar<fold text='<~>'><
    int i = 0
></fold> : Bar<fold text='<~>'><

></fold> <fold text='{...}'>{

}</fold>

defvar i = <fold text='[{...}]'>[{

}]</fold>;

multiclass Bar<fold text='<~>'><
    int i = 0
></fold> : Bar<fold text='<~>'><

></fold>

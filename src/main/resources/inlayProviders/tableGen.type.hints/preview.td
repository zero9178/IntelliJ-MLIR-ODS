class Op<string mnemonic> {
  string name = mnemonic;
}

class Dialect<list<Op> operations> {
  defvar /*<# list<|Op|> #>*/ops = operations;
  list<string> names = !foreach(/*<# Op #>*/op, ops, op.name);
}

foreach /*<# string #>*/name = ["add", "sub"] in
  def name : Op<name>;

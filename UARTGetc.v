
import "DPI-C" function void uart_getc(output byte ch);

module UARTGetc (
  input clk,
  input getc,
  output reg [7:0] ch
);

  always@(posedge clk) begin
    if (getc) uart_getc(ch);
  end

endmodule
     
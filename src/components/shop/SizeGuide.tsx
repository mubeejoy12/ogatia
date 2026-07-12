import type { ProductCategory } from "@/types/product";

type ChartRow = {
  size: string;
  measurements: Record<string, string>;
};

type Chart = {
  headers: string[];
  rows: ChartRow[];
  note: string;
};

/**
 * Size charts per category. Measurements in centimetres — the atelier
 * standard. When the accounts feature ships, we'll add a "cm / in" toggle
 * for international customers.
 */
const charts: Record<ProductCategory, Chart> = {
  Suits: {
    headers: ["Size", "Chest", "Waist", "Shoulder", "Sleeve"],
    rows: [
      { size: "46", measurements: { Chest: "92", Waist: "78", Shoulder: "44", Sleeve: "63" } },
      { size: "48", measurements: { Chest: "96", Waist: "82", Shoulder: "45", Sleeve: "63.5" } },
      { size: "50", measurements: { Chest: "100", Waist: "86", Shoulder: "46", Sleeve: "64" } },
      { size: "52", measurements: { Chest: "104", Waist: "90", Shoulder: "47", Sleeve: "64.5" } },
      { size: "54", measurements: { Chest: "108", Waist: "94", Shoulder: "48", Sleeve: "65" } },
      { size: "56", measurements: { Chest: "112", Waist: "98", Shoulder: "49", Sleeve: "65.5" } },
    ],
    note: "Measurements in centimetres. If you sit between two sizes, take the larger — the atelier will adjust down.",
  },
  Shirts: {
    headers: ["Size", "Chest", "Waist", "Neck", "Sleeve"],
    rows: [
      { size: "S", measurements: { Chest: "96", Waist: "82", Neck: "38", Sleeve: "63" } },
      { size: "M", measurements: { Chest: "100", Waist: "86", Neck: "40", Sleeve: "64" } },
      { size: "L", measurements: { Chest: "108", Waist: "94", Neck: "42", Sleeve: "65" } },
      { size: "XL", measurements: { Chest: "116", Waist: "102", Neck: "44", Sleeve: "66" } },
    ],
    note: "Measurements in centimetres. The house cut runs true to size.",
  },
  Trousers: {
    headers: ["Waist", "Hip", "Thigh", "Inseam"],
    rows: [
      { size: "30", measurements: { Hip: "94", Thigh: "58", Inseam: "82" } },
      { size: "32", measurements: { Hip: "98", Thigh: "60", Inseam: "82" } },
      { size: "34", measurements: { Hip: "102", Thigh: "62", Inseam: "84" } },
      { size: "36", measurements: { Hip: "106", Thigh: "64", Inseam: "84" } },
      { size: "38", measurements: { Hip: "110", Thigh: "66", Inseam: "86" } },
    ],
    note: "Waist in inches, remaining measurements in centimetres. Inseam can be adjusted at the atelier.",
  },
  Outerwear: {
    headers: ["Size", "Chest", "Shoulder", "Sleeve", "Length"],
    rows: [
      { size: "S", measurements: { Chest: "104", Shoulder: "46", Sleeve: "64", Length: "108" } },
      { size: "M", measurements: { Chest: "108", Shoulder: "47", Sleeve: "65", Length: "110" } },
      { size: "L", measurements: { Chest: "114", Shoulder: "48", Sleeve: "66", Length: "112" } },
      { size: "XL", measurements: { Chest: "120", Shoulder: "49", Sleeve: "67", Length: "114" } },
    ],
    note: "Measurements in centimetres. Cut to wear cleanly over full tailoring.",
  },
  Native: {
    headers: ["Size", "Chest", "Length (Kaftan)", "Length (Agbada)"],
    rows: [
      { size: "S", measurements: { Chest: "104", "Length (Kaftan)": "100", "Length (Agbada)": "128" } },
      { size: "M", measurements: { Chest: "110", "Length (Kaftan)": "102", "Length (Agbada)": "130" } },
      { size: "L", measurements: { Chest: "118", "Length (Kaftan)": "104", "Length (Agbada)": "132" } },
      { size: "XL", measurements: { Chest: "126", "Length (Kaftan)": "106", "Length (Agbada)": "134" } },
      { size: "XXL", measurements: { Chest: "134", "Length (Kaftan)": "108", "Length (Agbada)": "136" } },
    ],
    note: "Measurements in centimetres. Traditional cuts run generously through the body by design.",
  },
  Accessories: {
    headers: ["Size", "Note"],
    rows: [
      { size: "One Size", measurements: { Note: "Cut to fit — no sizing required." } },
    ],
    note: "",
  },
};

/**
 * A per-category size chart, rendered as a semantic table.
 *
 * Kept in an <details> element by default so the chart is fully searchable
 * and screen-reader accessible, but does not add visual weight above the
 * fold. Users open it when they need it.
 */
export function SizeGuide({ category }: { category: ProductCategory }) {
  const chart = charts[category];

  return (
    <details className="mt-12 border-t border-ink/10 pt-8 group">
      <summary className="cursor-pointer list-none flex items-center justify-between gap-4">
        <span className="eyebrow text-stone-500">Size guide</span>
        <span
          aria-hidden
          className="text-xs uppercase tracking-widest text-stone-500 group-open:hidden"
        >
          Open
        </span>
        <span
          aria-hidden
          className="text-xs uppercase tracking-widest text-stone-500 hidden group-open:inline"
        >
          Close
        </span>
      </summary>

      <div className="mt-6 overflow-x-auto">
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr>
              {chart.headers.map((h) => (
                <th
                  key={h}
                  scope="col"
                  className="text-left eyebrow text-stone-500 border-b border-ink/20 py-3 pr-4"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {chart.rows.map((row) => (
              <tr key={row.size} className="border-b border-ink/5">
                <th scope="row" className="text-left py-3 pr-4 font-display text-ink">
                  {row.size}
                </th>
                {chart.headers.slice(1).map((h) => (
                  <td key={h} className="py-3 pr-4 text-stone-700">
                    {row.measurements[h] ?? "—"}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {chart.note && (
        <p className="mt-4 text-xs text-stone-500 max-w-lg">{chart.note}</p>
      )}
    </details>
  );
}

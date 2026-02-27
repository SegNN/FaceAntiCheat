

package wtf.faceac.server;

public class AIResponse {
    private final double probability;
    private final String error;
    private final String model;
    private final String action;     // banned | kicked | flagged | watching (from backend)

    public AIResponse(double probability) {
        this(probability, null, null, null);
    }

    public AIResponse(double probability, String error) {
        this(probability, error, null, null);
    }

    public AIResponse(double probability, String error, String model) {
        this(probability, error, model, null);
    }

    public AIResponse(double probability, String error, String model, String action) {
        this.probability = probability;
        this.error = error;
        this.model = model;
        this.action = action;
    }

    public double getProbability() {
        return probability;
    }

    public String getError() {
        return error;
    }

    public String getModel() {
        return model != null ? model : "unknown";
    }

    public String getAction() {
        return action != null ? action : "watching";
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public static AIResponse fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            String trimmed = json.trim();

            int errorIndex = trimmed.indexOf("\"error\"");
            if (errorIndex != -1) {
                int colonIndex = trimmed.indexOf(':', errorIndex);
                if (colonIndex != -1) {
                    int start = trimmed.indexOf('"', colonIndex + 1);
                    if (start != -1) {
                        int end = trimmed.indexOf('"', start + 1);
                        if (end != -1) {
                            String errorMsg = trimmed.substring(start + 1, end);
                            return new AIResponse(0.0, errorMsg);
                        }
                    }
                }
            }

            int probIndex = trimmed.indexOf("\"probability\"");
            if (probIndex == -1) {
                return null;
            }
            int colonIndex = trimmed.indexOf(':', probIndex);
            if (colonIndex == -1) {
                return null;
            }
            int start = colonIndex + 1;
            while (start < trimmed.length() && Character.isWhitespace(trimmed.charAt(start))) {
                start++;
            }
            int end = start;
            while (end < trimmed.length()) {
                char c = trimmed.charAt(end);
                if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                    break;
                }
                end++;
            }
            String probStr = trimmed.substring(start, end);
            double probability = Double.parseDouble(probStr);

            String modelName = null;
            int modelIndex = trimmed.indexOf("\"model\"");
            if (modelIndex != -1) {
                int modelColonIndex = trimmed.indexOf(':', modelIndex);
                if (modelColonIndex != -1) {
                    int modelStart = trimmed.indexOf('"', modelColonIndex + 1);
                    if (modelStart != -1) {
                        int modelEnd = trimmed.indexOf('"', modelStart + 1);
                        if (modelEnd != -1) {
                            modelName = trimmed.substring(modelStart + 1, modelEnd);
                        }
                    }
                }
            }

            return new AIResponse(probability, null, modelName);
        } catch (Exception e) {
            return null;
        }
    }

    public String toJson() {
        return "{\"probability\":" + probability + "}";
    }

    @Override
    public String toString() {
        return "AIResponse{probability=" + probability + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        AIResponse that = (AIResponse) obj;
        return Double.compare(that.probability, probability) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(probability);
        return (int) (temp ^ (temp >>> 32));
    }
}
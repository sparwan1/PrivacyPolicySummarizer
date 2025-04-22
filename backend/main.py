from fastapi import FastAPI
from pydantic import BaseModel
import os
from dotenv import load_dotenv
import openai
from fastapi.middleware.cors import CORSMiddleware

load_dotenv()

app = FastAPI()

@app.get("/")
def read_root():
    return {"message": "Hello from FastAPI"}

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Set up Groq-compatible OpenAI client
client = openai.OpenAI(
    api_key=os.getenv("GROQ_API_KEY"),
    base_url="https://api.groq.com/openai/v1"
)

app = FastAPI()

class PolicyRequest(BaseModel):
    policy_text: str

@app.post("/analyze_policy")
async def analyze_policy(data: PolicyRequest):
    print("✅ Received POST request")
    
    # Temporarily truncate for safe token size
    truncated_text = data.policy_text[:5000]
    print(f"🧠 Policy text length: {len(truncated_text)} chars")

    full_prompt = f"""
Given the following privacy policy, analyze and classify the risk levels for the 10 factors listed below using the definitions provided.

---

### Privacy Policy:
{truncated_text}

---

### Risk Level Criteria:

1. *Email Address*  
   - Green: Not asked for  
   - Yellow: Used for the intended service  
   - Red: Shared with third parties  

2. *Credit Card Number and Home Address*  
   - Green: Not asked for  
   - Yellow: Used for the intended service  
   - Red: Shared with third parties  

3. *Social Security Number*  
   - Green: Not asked for  
   - Yellow: Used for the intended service  
   - Red: Shared with third parties  

4. *Ads and Marketing*  
   - Green: PII not used for marketing  
   - Yellow: PII used for marketing  
   - Red: PII shared for marketing  

5. *Location*  
   - Green: Not tracked  
   - Yellow: Used for the intended service  
   - Red: Shared with third parties  

6. *Collecting PII of Children*  
   - Green: Not collected  
   - Yellow: Not mentioned  
   - Red: Collected  

7. *Sharing with Law Enforcement*  
   - Green: PII not recorded  
   - Yellow: Legal documents required  
   - Red: Legal documents not required  

8. *Policy Change Notification*  
   - Green: Posted with opt-out option  
   - Yellow: Posted without opt-out  
   - Red: Not posted  

9. *Control of Data*  
   - Green: Edit and delete options available  
   - Yellow: Edit only  
   - Red: No edit/delete options  

10. *Data Aggregation*  
   - Green: Not aggregated  
   - Yellow: Aggregated without PII  
   - Red: Aggregated with PII  

---

### Output Format (JSON preferred):
```json
{{
  "Email Address": {{
    "risk_level": "Red",
    "justification": "The policy states that email addresses are shared with third-party partners.",
    "snippet": "We may share your email with third-party advertisers."
  }},
  ...
}}
"""
    try:
        response = client.chat.completions.create(
            model="deepseek-r1-distill-llama-70b",  # ✅ DeepSeek model
            messages=[
                {"role": "user", "content": full_prompt}
            ],
            temperature=0.2
        )

        print("✅ LLM responded successfully")
        return {"response": response.choices[0].message.content}

    except Exception as e:
        print("❌ LLM call failed:", e)
        return {"error": str(e)}

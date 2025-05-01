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
   Analyze the privacy policy below and assign risk levels for the 10 factors using the definitions provided.
   ---
   #Privacy Policy:
   {truncated_text}
   ---

   #Risk Level Criteria:
   1. Email Address
   Question to answer: How does the site handle your email address?
   - Green: Not collected | Yellow: Used for the intended service | Red: Shared w/ third parties

   2. Credit Card Number and Home Address
   Question to answer: How does the site handle your credit card/payment information and home address?
   - Green: Not asked for payment/transaction details and address | Yellow: Asked but used for the intended service | Red: Asked and also shared w/ third parties

   3. Social Security Number
   Question to answer:  How does the site handle your SSN?
   - Green: Not asked for | Yellow: Used for the intended service | Red: Shared w/ third parties

   4. Ads and Marketing
   Question to answer: Does the site use or share your PII for marketing purposes?
   - Green: PII not used | Yellow: PII used for marketing | Red: PII shared for marketing

   5. Location
   Question to answer: Does the site track or share your location?
   - Green: Not tracked | Yellow: Used for the intended service only | Red: shared w/ third parties 

   6. Collecting PII of Children
   Question to answer: Does the site collect personally identifiable information from children under 13?
   - Green: Not collected | Yellow: Not mentioned | Red: Collected

   7. Sharing with Law Enforcement  
   Question to answer: Does the site share your information with law enforcement?
   - Green: PII not recorded | Yellow: Legal documents required | Red: Legal documents not required  

   8. Policy Change Notification 
   Question to answer: Does the site notify you or allow you to opt out when their privacy policy changes?
   - Green: Posted with opt-out option | Yellow: Posted without opt-out | Red: Not posted

   9. Control of Data
   Question to ask: Does the site allow you to edit or delete your information from its records?
   - Green: Edit and delete options available | Yellow: Edit only | Red: No edit/delete options

   10. Data Aggregation*  
   Question to ask: Does the site collect or share aggregated data related to your identity or behavior?
   - Green: Not aggregated | Yellow: Aggregated without PII | Red: Aggregated with PII 
   ---

   #Output Format (in JSON):
   ```json
   {{
   "Email Address": {{
      "risk_level": "Red",
      "justification": "The policy states that...",
      "snippet": "We may share your email with third-party advertisers."
   }},
   ...
   }}
   """
    try:
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",  # ✅ DeepSeek model
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

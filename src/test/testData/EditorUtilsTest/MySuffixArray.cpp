#include <vector>
#include <string>
#define f first
#define s second


using namespace std;

struct SuffixArray {
    struct letter {
      int l,r,id;
    };
    
    int n;
    string str;
    vector<int> sa;
    vector<int> lcp;
    
    SuffixArray(string strr) : str(strr + '\0') {
		n = str.size();
        int maxSize = max(n+1, 256) + 1;
        str.resize(n);
        sa.resize(n);
        lcp.resize(n);        
        vector<letter> ar(n);
        vector<int> rnk(n);
        for (int i = 0; i < n;i++) {
            str[i] = strr[i];
            rnk[i] = str[i];
            ar[i].id = i;
        }
        
        int lg2 = 31-__builtin_clz(n);
        if (1LL<<lg2 < n) lg2++;

        vector<pair<int,int> > cnt1[maxSize], cnt2[maxSize];
        
        for (int p = 1; p <= lg2;p++) {
            for (int i = 0; i < n;i++) {
                ar[i].l = rnk[i];
                ar[i].r = (i+(1<<(p-1)) < n )?  rnk[i+(1<<(p-1))] : n+1;
                ar[i].id = i;
                cnt1[ar[i].r].push_back({ar[i].l,i});
            }
            for (int i = 0; i < maxSize;i++) {
                for (auto v : cnt1[i]) {
                    cnt2[v.f].push_back({i,v.s});
                }
            }
            int c = 0;
            for (int i = 0; i < maxSize;i++) {
                for (auto v : cnt2[i]) {
                    ar[c].l = i;
                    ar[c].r = v.f;
                    ar[c].id = v.s;
                    c++;
                }
                cnt1[i].clear();
                cnt2[i].clear();
            }

            for (int i = 0; i < n;i++) {
                if (i == 0) {
                    rnk[ar[i].id] = 1;
                }
                else {
                    if (ar[i-1].l == ar[i].l && ar[i-1].r == ar[i].r) {
                        rnk[ar[i].id] = rnk[ar[i-1].id];
                    }
                    else rnk[ar[i].id] = rnk[ar[i-1].id] + 1;
                }
            }
        }

        for (int i = 0; i < n;i++) {
            sa[i] = ar[i].id;
        }

        vector<int> inv_suff(n, 0);
    
        for (int i = 0; i < n; i++)
            inv_suff[sa[i]] = i;
        
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (inv_suff[i] == n-1) {
                k = 0;
                continue;
            }
            int j = sa[inv_suff[i]+1];
            while (i + k < n && j + k < n && str[i+k] == str[j+k])
                k++;
    
            lcp[inv_suff[i]] = k;
            if (k > 0) k--;
        }
    }

    pair<int, int> next(int L, int R, int i, char c) {
		int l = L, r = R+1;
		while (l < r) {
			int m = (l+r)/2;
			if (i+sa[m] >= n or str[i+sa[m]] < c) l = m+1;
			else r = m;
		}
		if (l == R+1 or str[i+sa[l]] > c) return {-1, -1};
		L = l;

		l = L, r = R+1;
		while (l < r) {
			int m = (l+r)/2;
			if (i+sa[m] >= n or str[i+sa[m]] <= c) l = m+1;
			else r = m;
		}
		R = l-1;
		return {L, R};
	}

	pair<int,int> range(string& t) {
		int L = 0, R = n-1;
		for (int i = 0; i < t.size(); i++) {
			tie(L, R) = next(L, R, i, t[i]);
			if (L == -1) return {-1,-1};
		}
		return {L, R};
	}
	
	int lower_bound(string t){
		int l = 1,r = n, n2 = t.size();
		while (l < r) {
			int m = (l+r)>>1;
			if(str.substr(sa[m],min(n-1-sa[m],n2+1)) >= t) r = m;
			else l = m+1;
		}
		return l;
	}

	int upper_bound(string t){
		int l = 1,r = n, n2 = t.size();
		while (l < r) {
			int m = (l+r)/2;
			if(str.substr(sa[m],min(n-1-sa[m],n2)) > t) r = m;
			else l = m+1;
		}
		return l;
	}
};
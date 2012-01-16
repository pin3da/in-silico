#include <stdlib.h>

#define MAX(x,y) ( ((x)>(y)) ? (x) : (y) )
#define MIN(x,y) ( ((x)<(y)) ? (x) : (y) )

#define REP(i,N) for((i)=0; (i)<(N); (i)++)
#define REPB(i,N) for((i)=(N)-1; (i)>=0; (i)--)

typedef unsigned int word;
typedef unsigned long long dword;

void copyw(word* res, word* a, word size) {
    int i;
    REP(i,size) {
        res[i] = a[i];
    }
}

void invStr(char* str, int n) {
    word i=0;
    word j=n-1;
    char tmp;
    while (i < j) {
        tmp = str[i];
        str[i] = str[j];
        str[j] = tmp;
        i++; j--;
    }
}

#define WBITS 32
#define WMASK ( ((dword)1 << WBITS) - 1
#define BN_NEG 0
#define BN_POS 1

/**
 * Structure that represents big integers, d[0] is the least significant
 * digit while d[size-1] is the most significant digit
 */
typedef struct {
    word maxSize; /*Maximum number of digits that d can hold*/
    word size; /*number of digits*/
    char sign; /*zero->negative, other->positive*/
    word *d; /*value of each digits*/
} BigInt;

BigInt* bnNewBigInt(word size, word initVal);
void bnDelBigInt(BigInt *a);
int bnUCompareInt(BigInt *a, BigInt *b);
void bnNegInt(BigInt *a);
void bnShiftLBits(BigInt *res, BigInt *a, word bits);
void bnShiftRBits(BigInt *res, BigInt *a, word bits);
void bnAddInt(BigInt *res, BigInt *a, BigInt *b);
void bnSubInt(BigInt *res, BigInt *a, BigInt *b);
void bnMulInt(BigInt *res, BigInt *a, BigInt *b);
void bnDivIntWord(BigInt *ans, BigInt *a, word b, word *res);
void bnDivInt(BigInt *ans, BigInt *a, BigInt *b, BigInt *res);
void bnPowInt(BigInt *ans, BigInt *a, int b);
void bnPowModInt(BigInt *ans, BigInt *a, BigInt* b, BigInt *mod);
void bnIntToStr(char *ans, BigInt *a);
void bnCopyInt(BigInt *res, BigInt *a);
void bnStrToInt(BigInt *ans, const char *input);

/*Removes leading ceros*/
void bnRemCeros(BigInt *a) {
    while (a->size > 1 && a->d[ a->size-1 ] == 0)
        a->size--;
}

/*Suma a y b, independiente de que signos tenga; Se debe cumplir a>=b*/
void bnUAddInt(BigInt *res, BigInt *a, BigInt *b) {
    word i,n,carry,tmp;
    dword r;
    n = a->size;
    carry=0;
    REP(i,n) {
    	tmp = (i < b->size) ? b->d[i] : 0;
        r = ((dword)a->d[i]) + tmp;
        r += carry;
        res->d[i] = (word)r;
        carry = (word)(r>>WBITS);
    }
    if (carry != 0) {
        res->d[n++] = carry;
    }
    res->size=n;
}

/*resta a y b, independiente del signo; Se debe cumplir que a >= b*/
void bnUSubInt(BigInt *res, BigInt *a, BigInt *b) {
    word borrow,i,n,tmp;
    dword s,m,base;
    n = a->size;
    borrow=0;
    base = ((dword)1 << WBITS);
    REP(i,n) {
    	tmp = (i < b->size) ? b->d[i] : 0;
        s = a->d[i];
        m = ((dword)tmp) + borrow;
        borrow=0;
        if (s < m) {
            s += base;
            borrow=1;
        }
        res->d[i] = (word)(s-m);
    }
    res->size=n;
    bnRemCeros(res);
}

/**
 * Returns positive if a is greater than b, 0 if a is equal to b
 * or negative if a is less than b
 */
int bnUCompareInt(BigInt* a, BigInt* b) {
    if (a->size != b->size)
        return ((int)a->size) - ((int)b->size);
    int i;
    REPB(i,a->size) {
        if (a->d[i] > b->d[i]) return 1;
        else if (a->d[i] < b->d[i]) return -1;
    }
    return 0;
}

void bnNegInt(BigInt* a) {
    if (a->sign == BN_NEG)
        a->sign=BN_POS;
    else
        a->sign=BN_NEG;
}

void bnShiftLBits(BigInt* res, BigInt* a, word bits) {
    word rdig[res->maxSize];
    word carry, shdig, shbits, i;
    dword lshift;
    carry = 0;
    shdig = bits/WBITS; shbits = bits%WBITS;
    REP(i,shdig) {
        rdig[i]=0;
    }
    REP(i,a->size) {
        lshift = a->d[i];
        lshift <<= shbits;
        lshift |= carry;
        rdig[i+shdig] = (word)lshift;
        carry = (word)(lshift >> WBITS);
    }
    rdig[i+shdig] = carry;
    res->size = a->size + shdig + 1;
    res->sign = a->sign;
    copyw(res->d, rdig, res->size);
    bnRemCeros(res);
}

void bnShiftRBits(BigInt* res, BigInt* a, word bits) {
    word rdig[res->maxSize];
    word carry, shdig, shbits, otbits, i, temp;
    dword rshift;
    carry = 0;
    shdig = bits/WBITS; shbits = bits%WBITS; otbits = (WBITS-shbits);
    if(shdig>res->maxSize) {
    	shdig = res->maxSize;
    }
    REP(i,shdig) {
        rdig[i]=0;
    }
    for(i = shdig; i < a->size-1; i++) {
        rshift = a->d[i+1];
        rshift = rshift << otbits;   
    	rdig[i-shdig] = (a->d[i] >> shbits) | ((word) rshift);
    }
    if(a->size<shdig+1) {
    	rdig[0] = 0;
    	res->size = 1;
    	res->sign = a->sign;
    }
    else {
    	rdig[a->size-shdig-1] = (a->d[a->size-1] >> shbits);
    	res->size = a->size-shdig;
    	res->sign = a->sign;
    }
    copyw(res->d, rdig, res->size);
    bnRemCeros(res);
}

/**
 * Computes res = sum1+sum2 for BigInts.
 */
void bnAddInt(BigInt* res, BigInt* sum1, BigInt* sum2) {
    BigInt *a, *b;
    if (bnUCompareInt(sum1, sum2) > 0) {
        a=sum1; b=sum2;
    } else {
        a=sum2; b=sum1;
    }
    if (a->sign == b->sign) {
        bnUAddInt(res,a,b);
        res->sign = a->sign;
    } else {
        bnUSubInt(res,a,b);
        res->sign = a->sign;
    }
}

/**
 * Computes res = a-b for BigInts
 */
void bnSubInt(BigInt* res, BigInt* a, BigInt* b) {
    bnNegInt(b);
    bnAddInt(res,a,b);
    bnNegInt(b);
}

BigInt* bnNewBigInt(word maxSize, word initVal) {
    BigInt *a = (BigInt*) malloc(sizeof(BigInt));
    a->size=1;
    a->maxSize = maxSize;
    a->sign=BN_POS;
    a->d = 0;
    a->d = (word*) malloc(maxSize * sizeof(word));
    a->d[0] = initVal;
    return a;
}

void bnDelBigInt(BigInt* a) {
    word *d = a->d;
    free(a);
    free(d);
}

void bnMulInt(BigInt* res, BigInt* a, BigInt* b) {
	word d1[res->maxSize]; d1[0]=0;
	BigInt tmp1; tmp1.size=1; tmp1.maxSize=res->maxSize; tmp1.sign=BN_POS; tmp1.d=d1;
	word d2[res->maxSize]; d2[0]=0;
	BigInt tmp2; tmp2.size=1; tmp2.maxSize=res->maxSize; tmp2.sign=BN_POS; tmp2.d=d2;
	
	BigInt *tmp= &tmp1, *sum = &tmp2;
    word i, j, carry;
    dword m;
    //tmp = bnNewBigInt(res->maxSize,0); sum = bnNewBigInt(res->maxSize,0);

    REP(i, b->size) {
        carry = 0;
        REP(j, a->size) {
            m = ((dword)b->d[i])*a->d[j];
            m += carry;
            tmp->d[j] = (word)m;
            carry = (word)(m >> WBITS);
        }
        tmp->d[j] = carry;
        tmp->size = a->size+1;
        bnRemCeros(tmp);
        bnShiftLBits(tmp,tmp,WBITS*i);
        bnAddInt(sum,sum,tmp);
    }
    //word *swap=res->d; res->d=sum->d; sum->d=swap;
    //res->size = sum->size;
    //bnDelBigInt(tmp); bnDelBigInt(sum);
    bnCopyInt(res, sum);
    res->sign = (a->sign == b->sign) ? BN_POS : BN_NEG;
}

/*ans = a / b ; res = a % b #División sin signo*/
void bnDivIntWord(BigInt* ans, BigInt* a, word b, word *res) {
    dword num;
    word carry; int i;
    carry = 0;
    REPB(i, a->size) {
        num = a->d[i] | ((dword)carry)<<WBITS;
        ans->d[i] = (word)(num / b);
        carry = (word)(num % b);
    }
    ans->size = a->size; bnRemCeros(ans);
    ans->sign = a->sign;
    if (res != 0) *res = carry; 
}

void bnIntToStr(char* ans, BigInt* x) {
	char strres[15];
	int j;
    word i=0, res;
    BigInt *a = bnNewBigInt(x->size, 0);
    bnRemCeros(x);
    bnCopyInt(a,x);
    while (a->size > 1 || a->d[0] >= 1000000000) {
        bnDivIntWord(a,a,1000000000,&res);
        for(j=8;j>=0;j--) {
        	ans[i++] = (res % 10) + '0';
        	res /= 10;
        }
    }
    word lsw = a->d[0];
    while (lsw > 0) {
    	ans[i++] = '0' + (lsw % 10);
    	lsw /= 10;
    }    
    if (x->sign==BN_NEG)
        ans[i++]='-';
    ans[i] = '\0';
    invStr(ans, i);
    bnDelBigInt(a);
}

void bnCopyInt(BigInt* res, BigInt* a) {
    copyw(res->d,a->d,a->size);
    res->size = a->size;
    res->sign = a->sign;
}

void bnDivInt(BigInt* ans, BigInt* a, BigInt* b, BigInt* res) {
    BigInt *tmp=0, *cos=0;
    int i,j,bit;
    char sign = (a->sign == b->sign) ? BN_POS : BN_NEG;

	word d1[res->maxSize]; d1[0]=0;
	BigInt tmp1; tmp1.size=1; tmp1.maxSize=res->maxSize; tmp1.sign=BN_POS; tmp1.d=d1;
	tmp = &tmp1;
	word d2[ans->maxSize]; d2[0]=0;
	BigInt tmp2; tmp2.size=1; tmp2.maxSize=ans->maxSize; tmp2.sign=BN_POS; tmp2.d=d2;
	cos = &tmp2;
	
    tmp->sign=sign;  cos->sign = sign;

    if (b->size == 1) {
        bnDivIntWord(cos,a,b->d[0],&(tmp->d[0]));
        tmp->size=1;
    } else {
        if (bnUCompareInt(a,b) < 0) {
            cos->d[0]=0; cos->size=1; tmp->size=a->size;
            copyw(tmp->d,a->d,a->size);
        } else {
        	word opt1 = b->size-1;
            REP(i,opt1) {
                tmp->d[i] = a->d[a->size + i - opt1];
            }
            tmp->size = b->size-1; cos->d[0]=0; cos->size=1;
            REPB(i,(a->size-tmp->size)) {
                REPB(j,WBITS) {
                    bit = (a->d[i]>>j) & 1;
                    bnShiftLBits(tmp,tmp,1);
                    tmp->d[0] |= bit;
                    bnShiftLBits(cos,cos,1);
                    if (bnUCompareInt(tmp,b) >= 0) {
                        cos->d[0] |= 1;
                        bnSubInt(tmp,tmp,b);
                    }
                }
            }
            bnRemCeros(tmp); bnRemCeros(cos);
        }
    }
    if (ans != 0) bnCopyInt(ans, cos);
    if (res != 0) bnCopyInt(res, tmp);
}

void bnPowInt(BigInt* ans, BigInt* a, int b) {
    if (b==0) {
        ans->d[0]=1; ans->size=1; ans->sign=BN_POS;
    } else {
        BigInt* tmp = bnNewBigInt(a->size * b, 0);
        bnPowInt(tmp, a, b/2);
        bnMulInt(tmp, tmp, tmp);
        if ( (b & 1) == 1 ) {
            bnMulInt(tmp, tmp, a);
        }
        bnCopyInt(ans, tmp);
        bnDelBigInt(tmp);
    }
}

void bnPowModInt(BigInt *ans, BigInt *a, BigInt* b, BigInt *mod) {
	ans->d[0]=1; ans->size=1;
	BigInt *exponent = bnNewBigInt(b->size+1, 0);
	BigInt *base = bnNewBigInt(MAX(mod->size,a->size) + 1, 0);
	BigInt *tmp1 = bnNewBigInt(2*MAX(mod->size,a->size) + 1, 0);
	bnCopyInt(exponent, b); bnCopyInt(base, a);
	while (exponent->size > 1 || exponent->d[0]>0) {
		if ((exponent->d[0] & 1) == 1) {
			bnMulInt(tmp1, ans, base);
			bnDivInt(tmp1, tmp1, mod, ans);
		}
		bnShiftRBits(exponent, exponent, 1);		
		bnMulInt(tmp1, base, base);
		bnDivInt(tmp1, tmp1, mod, base);			
	}
	bnDelBigInt(exponent); bnDelBigInt(base); bnDelBigInt(tmp1);
}

void bnStrToInt(BigInt *ans, const char *input) {
    BigInt *tmp = bnNewBigInt(1,0);
    ans->size = 1; ans->d[0] = 0;
    int i=0,j; word w;
    int pow10[10]={1,10,100,1000,10000,100000,1000000,10000000,100000000,1000000000};
    if (input[0]=='-') {
        i++;
        ans->sign=BN_NEG;
    }
    while (input[i] != '\0') {
    	for (j=0, w=0; j<9; j++, i++) {
    		if(input[i] == '\0') break;
    		w *= 10; w += input[i]-0x30;
    	}
    	tmp->d[0]=pow10[j];
    	bnMulInt(ans, ans, tmp);
    	tmp->d[0]=w;
    	bnAddInt(ans, ans, tmp);        
    }
    bnDelBigInt(tmp);
}

